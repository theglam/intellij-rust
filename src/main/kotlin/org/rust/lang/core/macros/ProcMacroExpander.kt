/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.rust.cargo.toolchain.tools.Rustup
import org.rust.lang.core.macros.tt.*
import org.rust.lang.core.parser.createRustPsiBuilder
import org.rust.stdext.RsResult
import java.io.*
import java.lang.reflect.Type

class ProcMacroExpander(
    private val project: Project,
    private val server: ProcMacroServer
) : MacroExpander<RsProcMacroData>() {
    override fun expandMacroAsText(def: RsProcMacroData, call: RsMacroCallData): Pair<CharSequence, RangeMap>? {
        val macroCallBodyText = call.macroBody ?: return null
        val macroCallBody = project.createRustPsiBuilder(macroCallBodyText).parseSubtree()
        return expand(macroCallBody, def.name, def.artifact.path.toString()).map {
            val toText = it.toText()
            println(toText)
            toText to RangeMap.EMPTY
        }.ok()
    }

    fun expand(macroCallBody: SubtreeS, macroName: String, lib: String): RsResult<SubtreeS, ResponseError> {
        val response = server.send(Request.ExpansionMacro(ExpansionTask(macroCallBody, macroName, null, lib)))
        return when (response) {
            is Response.ExpansionMacro -> RsResult.Ok(response.expansionResult.expansion)
            is Response.Error -> RsResult.Err(response.error)
        }
    }
}

private fun SubtreeS.toText(): CharSequence {
    val sb = StringBuilder()
    appendTo(sb)
    return sb
}

private fun SubtreeS.appendTo(sb: StringBuilder) {
    delimiter?.let { sb.append(it.kind.openText) }
    for (tokenTree in tokenTrees) {
        when (tokenTree) {
            is TokenTree.Leaf -> tokenTree.leaf.appendTo(sb)
            is TokenTree.Subtree -> tokenTree.subtree.appendTo(sb)
        }
    }
    delimiter?.let { sb.append(it.kind.closeText) }
}

private fun LeafS.appendTo(sb: StringBuilder) {
    when (this) {
        is LeafS.Literal -> sb.append(literal.text)
        is LeafS.Ident -> {
            sb.append(ident.text)
            sb.append(" ")
        }
        is LeafS.Punct -> {
            sb.append(punct.char)
            if (punct.spacing == Spacing.Alone) {
                sb.append(" ")
            }
        }
    }
}

sealed class Request {
    data class ExpansionMacro(@SerializedName("ExpansionMacro") val expansionTask: ExpansionTask): Request()
}

data class ExpansionTask(
    @SerializedName("macro_body")
    val macroBody: SubtreeS,
    @SerializedName("macro_name")
    val macroName: String,
    val attributes: SubtreeS?,
    val lib: String,
)

sealed class Response {
    data class Error(@SerializedName("Error") val error: ResponseError) : Response()
//    data class ListMacro(@SerializedName("ListMacro") val listMacroResult: ListMacrosResult) : Response()
    data class ExpansionMacro(@SerializedName("ExpansionMacro") val expansionResult: ExpansionResult1) : Response()
}

class ResponseAdapter : JsonSerializer<Response>, JsonDeserializer<Response> {
    override fun serialize(json: Response, type: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(json, json.javaClass)
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Response? {
        val obj = json.asJsonObject
        val concreteType = when {
            obj.has("Error") -> Response.Error::class.java
            obj.has("ExpansionMacro") -> Response.ExpansionMacro::class.java
            else -> return null
        }
        return context.deserialize(json, concreteType)
    }
}

data class ExpansionResult1(val expansion: SubtreeS)

data class ResponseError (
    val code: ErrorCode,
    val message: String,
)

enum class ErrorCode {
    ServerErrorEnd,
    ExpansionError,
}

class ProcMacroServer(
    private val process: Process,
    private val stdin: Writer,
    private val stdout: Reader,
) {
    fun send(request: Request): Response {
        val gson = GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(Response::class.java, ResponseAdapter())
            .registerTypeAdapter(TokenTree::class.java, TokenTreeAdapter())
            .registerTypeAdapter(LeafS::class.java, LeafSAdapter())
            .create()
        stdin.write(gson.toJson(request))
        stdin.write("\n")
        stdin.flush()

//        Thread.sleep(1000)
//
//        while (process.errorStream.available() > 0) {
//            println(InputStreamReader(process.errorStream).readCharSequence())
//        }
//
//        while (true) {
//            print(stdout.read().toChar())
//        }

        // {"ExpansionMacro":{"expansion":{"delimiter":null,"token_trees":[{"Leaf":{"Punct":{"char":".","spacing":"Alone","id":0}}}]}}}
        // {"Error":{"code":"ExpansionError","message":"Cannot perform expansion for do_panic: error String(\"use-after-free in `proc_macro` handle\")"}}

        return gson.fromJson(gson.newJsonReader(stdout), Response::class.java)
    }

    companion object {
        private val LOG: Logger = Logger.getInstance("org.rust.macros.ProcMacroServer")

        fun connect(rustup: Rustup): ProcMacroServer? {
            val process: Process = try {
                ProcessBuilder("${rustup.executable} run nightly rust-analyzer proc-macro".split(" "))
//                ProcessBuilder("/home/vlad20012/workspace/intellij-rust/proc_macro_expander/target/release/proc_macro_expander")
                    .start()
            } catch (e: IOException) {
                LOG.error(e)
                return null
            }

            return ProcMacroServer(process, OutputStreamWriter(process.outputStream), InputStreamReader(process.inputStream))
        }
    }
}
