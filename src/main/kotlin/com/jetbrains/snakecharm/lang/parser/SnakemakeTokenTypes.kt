package com.jetbrains.snakecharm.lang.parser

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.psi.PyElementType

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
object SnakemakeTokenTypes {
    val RULE_KEYWORD = PyElementType("RULE_KEYWORD") // rule
    val CHECKPOINT_KEYWORD = PyElementType("CHECKPOINT_KEYWORD") // rule

    val WORKFLOW_CONFIGFILE_KEYWORD = PyElementType("WORKFLOW_CONFIGFILE_KEYWORD")
    val WORKFLOW_REPORT_KEYWORD = PyElementType("WORKFLOW_REPORT_KEYWORD")
    val WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD = PyElementType("WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD")
    val WORKFLOW_SINGULARITY_KEYWORD = PyElementType("WORKFLOW_SINGULARITY_KEYWORD")
    val WORKFLOW_INCLUDE_KEYWORD = PyElementType("WORKFLOW_INCLUDE_KEYWORD")
    val WORKFLOW_WORKDIR_KEYWORD = PyElementType("WORKFLOW_WORKDIR_KEYWORD")

    val WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS = TokenSet.create(
            WORKFLOW_CONFIGFILE_KEYWORD,
            WORKFLOW_REPORT_KEYWORD,
            WORKFLOW_SINGULARITY_KEYWORD,
            WORKFLOW_INCLUDE_KEYWORD,
            WORKFLOW_WORKDIR_KEYWORD,
            WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD
    )

    val WORKFLOW_LOCALRULES_KEYWORD = PyElementType("WORKFLOW_LOCALRULES_KEYWORD")
    val WORKFLOW_RULEORDER_KEYWORD = PyElementType("WORKFLOW_RULEORDER_KEYWORD")

    val WORKFLOW_ONSUCCESS_KEYWORD = PyElementType("WORKFLOW_ONSUCCESS_KEYWORD")
    val WORKFLOW_ONERROR_KEYWORD = PyElementType("WORKFLOW_ONERROR_KEYWORD")
    val WORKFLOW_ONSTART_KEYWORD = PyElementType("WORKFLOW_ONSTART_KEYWORD")
    val WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS = TokenSet.create(
            WORKFLOW_ONSUCCESS_KEYWORD, WORKFLOW_ONERROR_KEYWORD, WORKFLOW_ONSTART_KEYWORD
    )

    val WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE = TokenSet.orSet(
            WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS,
            WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS,
            TokenSet.create(
                    WORKFLOW_LOCALRULES_KEYWORD, WORKFLOW_RULEORDER_KEYWORD
            ))

    val RULE_LIKE = TokenSet.create(
            RULE_KEYWORD, CHECKPOINT_KEYWORD
    )

    val WORKFLOW_TOPLEVEL_DECORATORS = TokenSet.orSet(
            WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE,
            RULE_LIKE
    )
}