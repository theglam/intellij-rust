/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt

import com.intellij.lang.PsiBuilder
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.rust.lang.core.macros.FragmentKind
import org.rust.lang.core.parser.RustParserUtil
import org.rust.lang.core.psi.MacroBraces
import org.rust.lang.core.psi.RS_COMMENTS
import org.rust.lang.core.psi.RS_LITERALS
import org.rust.lang.core.psi.RsElementTypes.*
import org.rust.lang.core.psi.tokenSetOf

fun PsiBuilder.parseSubtree(): SubtreeS {
    return SubtreeParser(this).parse()
}

private class SubtreeParser(
    private val lexer: PsiBuilder
) {
    var counter = 0;

    fun parse(): SubtreeS {
        val result = mutableListOf<TokenTree>()

        while (true) {
            val tokenType = lexer.tokenType ?: break
            val offset = lexer.currentOffset

            collectLeaf(tokenType, result)
        }

        if (result.size == 1 && result.single() is TokenTree.Subtree) {
            return (result.single() as TokenTree.Subtree).subtree
        }

        return SubtreeS(null, result)
    }

    private fun collectLeaf(tokenType: IElementType, result: MutableList<TokenTree>) {
        val delimKind = MacroBraces.fromOpenToken(tokenType)
        if (delimKind != null) {
            val delimLeaf = Delimiter(counter++, delimKind)
            val subtreeResult = mutableListOf<TokenTree>()
            lexer.advanceLexer()
            while (true) {
                val tokenType2 = lexer.tokenType ?: break
                if (tokenType2 == delimKind.closeToken) break
                val offset2 = lexer.currentOffset

                collectLeaf(tokenType2, subtreeResult)
            }
            result += TokenTree.Subtree(SubtreeS(delimLeaf, subtreeResult))
        } else {
            val tokenText = lexer.tokenText!!
            when (tokenType) {
                INTEGER_LITERAL -> {
                    val tokenText2 = if (RustParserUtil.parseFloatLiteral(lexer, 0)) {
                        val m = lexer.latestDoneMarker!!
                        lexer.originalText.substring(m.startOffset, m.endOffset)
                    } else {
                        tokenText
                    }
                    result += TokenTree.Leaf(LeafS.Literal(LiteralS(tokenText2, counter++)))
                }
                in RS_LITERALS -> result += TokenTree.Leaf(LeafS.Literal(LiteralS(tokenText, counter++)))
                in FragmentKind.IDENTIFIER_TOKENS -> result += TokenTree.Leaf(LeafS.Ident(IdentS(tokenText, counter++)))
                QUOTE_IDENTIFIER -> {
                    result += TokenTree.Leaf(LeafS.Punct(PunctS(tokenText[0].toString(), Spacing.Joint, counter++)))
                    result += TokenTree.Leaf(LeafS.Ident(IdentS(tokenText.substring(1), counter++)))
                }
                else -> {
                    for (i in tokenText.indices) {
                        val isLast = i == tokenText.lastIndex
                        val char = tokenText[i].toString()
                        val spacing = when {
                            !isLast -> Spacing.Joint
                            else -> {
                                val next = lexer.rawLookup(1)
                                when {
                                    next == null -> Spacing.Alone
                                    next in SET -> Spacing.Alone
                                    next !in RS_LITERALS && next !in FragmentKind.IDENTIFIER_TOKENS -> Spacing.Joint
                                    else -> Spacing.Alone
                                }
                            }
                        }
                        result += TokenTree.Leaf(LeafS.Punct(PunctS(char, spacing, counter++)))
                    }
                }
            }
        }

        lexer.advanceLexer()
    }
}

private val SET = TokenSet.orSet(tokenSetOf(WHITE_SPACE), RS_COMMENTS, tokenSetOf(LBRACK, LBRACE, LPAREN))
