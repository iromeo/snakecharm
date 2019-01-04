package com.jetbrains.snakemake.lang.parser

import com.jetbrains.python.PyBundle.message
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.ExpressionParsing
import com.jetbrains.python.parsing.Parsing

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SnakemakeExpressionParsing(context: SnakemakeParserContext): ExpressionParsing(context) {
    override fun getParsingContext() = myContext as SnakemakeParserContext

    fun parseRuleParamArgumentList(): Boolean {
        val argList = myBuilder.mark()

        val argsOnNextLine = myBuilder.tokenType === PyTokenTypes.STATEMENT_BREAK
        if (argsOnNextLine) {
            nextToken()
            if (!checkMatches(PyTokenTypes.INDENT, "Indent expected...")) { // bundle
                argList.done(PyElementTypes.ARGUMENT_LIST)
                return false
            }
        }
        var argNumber = 0
        while (!myBuilder.eof() && myBuilder.tokenType !== PyTokenTypes.STATEMENT_BREAK) {
            argNumber++

            // comma if several args:
            if (argNumber > 1) {
                if (matchToken(PyTokenTypes.COMMA)) {
                    // if args on next line, not on this:
                    if (myBuilder.tokenType == PyTokenTypes.STATEMENT_BREAK) {
                        nextToken()
                    }
                    // TODO: cleanup
                    //  if (atToken(PyTokenTypes.STATEMENT_BREAK)) {
                    //      break
                    //  }
                } else {
                    myBuilder.error(message("PARSE.expected.comma.or.rpar"))
                    break
                }

            }

            // *args or **kw
            if (myBuilder.tokenType === PyTokenTypes.MULT || myBuilder.tokenType === PyTokenTypes.EXP) {
                val starArgMarker = myBuilder.mark()
                myBuilder.advanceLexer()
                if (!parseSingleExpression(false)) {
                    myBuilder.error(message("PARSE.expected.expression"))
                }
                starArgMarker.done(PyElementTypes.STAR_ARGUMENT_EXPRESSION)
            } else {
                // arg or named arg:
                if (Parsing.isIdentifier(myBuilder)) {
                    val keywordArgMarker = myBuilder.mark()
                    Parsing.advanceIdentifierLike(myBuilder)
                    if (myBuilder.tokenType === PyTokenTypes.EQ) {
                        myBuilder.advanceLexer()
                        if (!parseSingleExpression(false)) {
                            myBuilder.error(message("PARSE.expected.expression"))
                        }
                        keywordArgMarker.done(PyElementTypes.KEYWORD_ARGUMENT_EXPRESSION)
                        continue
                    }
                    keywordArgMarker.rollbackTo()
                }
                if (!parseSingleExpression(false)) {
                    myBuilder.error(message("PARSE.expected.expression"))
                    break
                }
            }
        }
        nextToken()
        if (argsOnNextLine) {
            checkMatches(PyTokenTypes.DEDENT, "Dedent expected")
        }
        argList.done(PyElementTypes.ARGUMENT_LIST)

        return true
    }

}