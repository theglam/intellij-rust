/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt

import com.google.gson.GsonBuilder
import org.intellij.lang.annotations.Language
import org.rust.RsTestBase
import org.rust.lang.core.parser.createRustPsiBuilder

class TtSerializationTest : RsTestBase() {
    fun `test 0`() = doTest(".", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 0
                }
              }
            }
          ]
        }
    """)

    fun `test 00`() = doTest("..", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 1
                }
              }
            }
          ]
        }
    """)

    fun `test 000`() = doTest(".foo", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Ident": {
                  "text": "foo",
                  "id": 1
                }
              }
            }
          ]
        }
    """)

    fun `test 0000`() = doTest(":::", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ":",
                  "spacing": "Joint",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ":",
                  "spacing": "Joint",
                  "id": 1
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ":",
                  "spacing": "Alone",
                  "id": 2
                }
              }
            }
          ]
        }
    """)

    fun `test 1`() = doTest(". asd .. \"asd\" ...", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Ident": {
                  "text": "asd",
                  "id": 1
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 2
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 3
                }
              }
            },
            {
              "Leaf": {
                "Literal": {
                  "text": "\"asd\"",
                  "id": 4
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 5
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 6
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 7
                }
              }
            }
          ]
        }
    """)

    fun `test 000022`() = doTest("{}", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Brace"
          },
          "token_trees": []
        }
    """)

    fun `test 000022 2`() = doTest("[]", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Bracket"
          },
          "token_trees": []
        }
    """)

    fun `test 000022 3`() = doTest("()", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Parenthesis"
          },
          "token_trees": []
        }
    """)

    fun `test 000023`() = doTest("(..)", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Parenthesis"
          },
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 1
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 2
                }
              }
            }
          ]
        }
    """)

    fun doTest(code: String, @Language("Json") expectedJson: String) {
        val subtree = project.createRustPsiBuilder(code).parseSubtree()
        val actualJson = GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create()
            .toJson(subtree)
        assertEquals(expectedJson.trimIndent(), actualJson)
    }
}
