/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.uast.kotlin

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.uast.*
import org.jetbrains.uast.java.AbstractJavaUVariable
import org.jetbrains.uast.java.JavaAbstractUExpression
import org.jetbrains.uast.java.JavaUAnnotation
import org.jetbrains.uast.java.annotations
import org.jetbrains.uast.kotlin.declarations.UastLightIdentifier
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable

abstract class AbstractKotlinUVariable : AbstractJavaUVariable() {
    override val uastInitializer: UExpression?
        get() {
            val psi = psi
            val initializerExpression = when (psi) {
                is UastKotlinPsiVariable -> psi.ktInitializer
                is UastKotlinPsiParameter -> psi.ktDefaultValue
                is KtLightElement<*, *> -> {
                    val origin = psi.kotlinOrigin
                    when (origin) {
                        is KtVariableDeclaration -> origin.initializer
                        is KtParameter -> origin.defaultValue
                        else -> null
                    }
                }
                else -> null
            } ?: return null
            return getLanguagePlugin().convertElement(initializerExpression, this) as? UExpression ?: UastEmptyExpression
        }

    override fun getNameIdentifier(): PsiIdentifier {
        val kotlinOrigin = (psi as? KtLightElement<*, *>)?.kotlinOrigin
        return UastLightIdentifier(psi, kotlinOrigin as KtNamedDeclaration?)
    }
}

class KotlinUVariable(
        psi: PsiVariable,
        override val uastParent: UElement?
) : AbstractKotlinUVariable(), UVariable, PsiVariable by psi {
    override val psi = unwrap<UVariable, PsiVariable>(psi)

    override val annotations by lz { psi.annotations.map { JavaUAnnotation(it, this) } }
    override val typeReference by lz { getLanguagePlugin().convertOpt<UTypeReferenceExpression>(psi.typeElement, this) }

    override fun getContainingFile(): PsiFile? = (psi as? KtLightElement<*, *>)?.kotlinOrigin?.containingFile ?: psi.containingFile

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    companion object {
        fun create(psi: PsiVariable, containingElement: UElement?): UVariable {
            return when (psi) {
                is PsiEnumConstant -> KotlinUEnumConstant(psi, containingElement)
                is PsiLocalVariable -> KotlinULocalVariable(psi, containingElement)
                is PsiParameter -> KotlinUParameter(psi, containingElement)
                is PsiField -> KotlinUField(psi, containingElement)
                else -> KotlinUVariable(psi, containingElement)
            }
        }
    }
}

open class KotlinUParameter(
        psi: PsiParameter,
        override val uastParent: UElement?
) : AbstractKotlinUVariable(), UParameter, PsiParameter by psi {

    override val psi = unwrap<UParameter, PsiParameter>(psi)

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }
}

open class KotlinUField(
        psi: PsiField,
        override val uastParent: UElement?
) : AbstractKotlinUVariable(), UField, PsiField by psi {

    override val psi = unwrap<UField, PsiField>(psi)

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }
}

open class KotlinULocalVariable(
        psi: PsiLocalVariable,
        override val uastParent: UElement?
) : AbstractKotlinUVariable(), ULocalVariable, PsiLocalVariable by psi {

    override val psi = unwrap<ULocalVariable, PsiLocalVariable>(psi)

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }
}

open class KotlinUEnumConstant(
        psi: PsiEnumConstant,
        override val uastParent: UElement?
) : AbstractKotlinUVariable(), UEnumConstant, PsiEnumConstant by psi {
    override val initializingClass: UClass? by lz { getLanguagePlugin().convertOpt<UClass>(psi.initializingClass, this) }

    override val psi = unwrap<UEnumConstant, PsiEnumConstant>(psi)

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override val kind: UastCallKind
        get() = UastCallKind.CONSTRUCTOR_CALL
    override val receiver: UExpression?
        get() = null
    override val receiverType: PsiType?
        get() = null
    override val methodIdentifier: UIdentifier?
        get() = null
    override val classReference: UReferenceExpression?
        get() = KotlinEnumConstantClassReference(psi, this)
    override val typeArgumentCount: Int
        get() = 0
    override val typeArguments: List<PsiType>
        get() = emptyList()
    override val valueArgumentCount: Int
        get() = psi.argumentList?.expressions?.size ?: 0

    override val valueArguments by lz {
        psi.argumentList?.expressions?.map {
            getLanguagePlugin().convertElement(it, this) as? UExpression ?: UastEmptyExpression
        } ?: emptyList()
    }

    override val returnType: PsiType?
        get() = psi.type

    override fun resolve() = psi.resolveMethod()

    override val methodName: String?
        get() = null

    private class KotlinEnumConstantClassReference(
            override val psi: PsiEnumConstant,
            override val uastParent: UElement?
    ) : JavaAbstractUExpression(), USimpleNameReferenceExpression {
        override fun resolve() = psi.containingClass
        override val resolvedName: String?
            get() = psi.containingClass?.name
        override val identifier: String
            get() = psi.containingClass?.name ?: "<error>"
    }
}