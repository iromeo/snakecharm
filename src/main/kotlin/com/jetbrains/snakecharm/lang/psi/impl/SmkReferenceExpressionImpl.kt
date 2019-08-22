package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil.getIdentifierNode
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType


class SmkReferenceExpressionImpl(node: ASTNode): PyElementImpl(node), SmkReferenceExpression {
    override fun getName() = getNameNode()?.text

    override fun setName(name: String): PsiElement {
        val nameElement = PyUtil.createNewName(this, name)
        val nameNode = getNameNode()
        if (nameNode != null) {
            node.replaceChild(nameNode, nameElement)
        }
        return this
    }

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null

    override fun getReference(): PsiReference? = SmkRuleOrCheckpointNameReference(this, TextRange(0, textLength))

    private fun getNameNode() = getIdentifierNode(node)

    class SmkRuleOrCheckpointNameReference(
            element: PsiNamedElement,
            textRange: TextRange
    ) : PsiReferenceBase.Poly<PsiNamedElement>(element, textRange, true) {
        private val key: String = element.text

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val rules = AbstractSmkRuleOrCheckpointType
                    .findAvailableRuleLikeElementByName(element, key, SmkRuleNameIndex.KEY, SmkRule::class.java)
                    { getRules().map{ (_, psi) -> psi }}
            val checkpoints = AbstractSmkRuleOrCheckpointType
                    .findAvailableRuleLikeElementByName(element, key, SmkCheckpointNameIndex.KEY, SmkCheckPoint::class.java)
                    { getCheckpoints().map{ (_, psi) -> psi }}

            return (rules + checkpoints)
                    .map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
                    .toTypedArray()
        }

        override fun handleElementRename(newElementName: String): PsiElement =
                element.setName(newElementName)

        private fun getRules() = PsiTreeUtil.getParentOfType(element, SmkFile::class.java)
                ?.collectRules() ?: emptyList()

        private fun getCheckpoints() = PsiTreeUtil.getParentOfType(element, SmkFile::class.java)
                ?.collectCheckPoints()
                ?: emptyList()
    }
}

