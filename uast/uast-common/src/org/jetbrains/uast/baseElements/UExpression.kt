/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.uast.internal.log
import org.jetbrains.uast.visitor.UastTypedVisitor
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents an expression or statement (which is considered as an expression in Uast).
 */
interface UExpression : UElement, UAnnotated {
    /**
     * Returns the expression value or null if the value can't be calculated.
     */
    fun evaluate(): Any? = null

    /**
     * Returns expression type, or null if type can not be inferred, or if this expression is a statement.
     */
    fun getExpressionType(): PsiType? = null

    override fun accept(visitor: UastVisitor) {
        visitor.visitElement(this)
        visitor.afterVisitElement(this)
    }

    override fun <D, R> accept(visitor: UastTypedVisitor<D, R>, data: D) = visitor.visitExpression(this, data)
}

/**
 * Represents an annotated element.
 */
interface UAnnotated : UElement {
    /**
     * Returns the list of annotations applied to the current element.
     */
    val annotations: List<UAnnotation>

    /**
     * Looks up for annotation element using the annotation qualified name.
     *
     * @param fqName the qualified name to search
     * @return the first annotation element with the specified qualified name, or null if there is no annotation with such name.
     */
    fun findAnnotation(fqName: String): UAnnotation? = annotations.firstOrNull { it.qualifiedName == fqName }
}

/**
 * Represents a labeled element.
 */
interface ULabeled : UElement {
    /**
     * Returns the label name, or null if the label is empty.
     */
    val label: String?

    /**
     * Returns the label identifier, or null if the label is empty.
     */
    val labelIdentifier: UIdentifier?
}

/**
 * In some cases (user typing, syntax error) elements, which are supposed to exist, are missing.
 * The obvious example - the lack of the condition expression in [UIfExpression], e.g. 'if () return'.
 * [UIfExpression.condition] is required to return not-null values,
 *  and Uast implementation should return something instead of 'null' in this case.
 *
 * Use [UastEmptyExpression] in this case.
 */
object UastEmptyExpression : UExpression {
    override val uastParent: UElement?
        get() = null

    override val annotations: List<UAnnotation>
        get() = emptyList()

    override val psi: PsiElement?
        get() = null

    override fun asLogString() = log()
}