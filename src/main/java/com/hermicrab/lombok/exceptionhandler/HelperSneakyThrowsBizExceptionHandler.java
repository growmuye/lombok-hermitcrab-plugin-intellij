package com.hermicrab.lombok.exceptionhandler;

import java.util.Collection;
import java.util.List;

import com.hermicrab.lombok.util.PsiAnnotationSearchUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.codeInsight.CustomExceptionHandler;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;

public class HelperSneakyThrowsBizExceptionHandler extends CustomExceptionHandler {

  private static final String TARET_EXCEPTION = "com.liepin.swift.core.exception.BizException";

  @Override
  public boolean isHandled(@Nullable PsiElement element, @NotNull PsiClassType exceptionType, PsiElement topElement) {
    PsiElement parent = PsiTreeUtil.getParentOfType(element, PsiLambdaExpression.class, PsiTryStatement.class, PsiMethod.class);
    if (parent instanceof PsiLambdaExpression) {
      // lambda it's another scope, @HelperSneakyThrowsBizExceptionHandler annotation can't neglect exceptions in lambda only on method, constructor
      return false;
    } else if (parent instanceof PsiTryStatement && isHandledByTryCatch(exceptionType, (PsiTryStatement) parent)) {
      // that exception MAY be already handled by regular try-catch statement
      return false;
    }

    if (topElement instanceof PsiTryStatement && isHandledByTryCatch(exceptionType, (PsiTryStatement) topElement)) {
      // that exception MAY be already handled by regular try-catch statement (don't forget about nested try-catch)
      return false;
    } else if (!(topElement instanceof PsiCodeBlock)) {
      final PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
      return psiMethod != null && isExceptionHandled(psiMethod, exceptionType);
    }
    return false;
  }

  private boolean isHandledByTryCatch(@NotNull PsiClassType exceptionType, PsiTryStatement topElement) {
    List<PsiType> caughtExceptions = ContainerUtil.map(topElement.getCatchBlockParameters(), PsiParameter::getType);
    return isExceptionHandled(exceptionType, caughtExceptions);
  }

  private boolean isExceptionHandled(@NotNull PsiModifierListOwner psiModifierListOwner, PsiClassType exceptionClassType) {
    final PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiModifierListOwner, "lombok.extern.liepin.HelperSneakyThrowsBizException");
    return psiAnnotation != null;
  }

  private boolean isExceptionHandled(@NotNull PsiClassType exceptionClassType, @NotNull Collection<PsiType> sneakedExceptionTypes) {
    for (PsiType sneakedExceptionType : sneakedExceptionTypes) {
      if (sneakedExceptionType.equalsToText(TARET_EXCEPTION)) {
        return true;
      }
    }

    final PsiClass unhandledExceptionClass = exceptionClassType.resolve();

    if (null != unhandledExceptionClass) {
      for (PsiType sneakedExceptionType : sneakedExceptionTypes) {
        if (sneakedExceptionType instanceof PsiClassType) {
          final PsiClass sneakedExceptionClass = ((PsiClassType) sneakedExceptionType).resolve();

          if (null != sneakedExceptionClass && unhandledExceptionClass.isInheritor(sneakedExceptionClass, true)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
