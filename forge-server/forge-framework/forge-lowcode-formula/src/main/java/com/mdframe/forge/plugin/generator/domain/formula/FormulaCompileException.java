package com.mdframe.forge.plugin.generator.domain.formula;

import com.googlecode.aviator.exception.CompileExpressionErrorException;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exception thrown when Aviator expression compilation fails.
 * Carries position information (line, column) extracted from Aviator error messages.
 */
public class FormulaCompileException extends RuntimeException {

    private static final Pattern POSITION_PATTERN =
        Pattern.compile("\\((?:(\\d+):(\\d+))\\)");

    private final int errorLine;
    private final int errorColumn;

    public FormulaCompileException(String message, Throwable cause, int errorLine, int errorColumn) {
        super(message, cause);
        this.errorLine = errorLine;
        this.errorColumn = errorColumn;
    }

    public int getErrorLine() { return errorLine; }
    public int getErrorColumn() { return errorColumn; }
    public boolean hasPosition() { return errorLine >= 0 && errorColumn >= 0; }

    /**
     * Create from Aviator's ExpressionSyntaxErrorException, extracting position.
     */
    static FormulaCompileException fromSyntaxError(ExpressionSyntaxErrorException e) {
        int[] pos = extractPosition(e.getMessage());
        return new FormulaCompileException(e.getMessage(), e, pos[0], pos[1]);
    }

    /**
     * Create from Aviator's CompileExpressionErrorException, extracting position.
     */
    static FormulaCompileException fromCompileError(CompileExpressionErrorException e) {
        int[] pos = extractPosition(e.getMessage());
        return new FormulaCompileException(e.getMessage(), e, pos[0], pos[1]);
    }

    /**
     * Convert to a SyntaxValidationResult for the validation service.
     */
    AviatorAdapter.SyntaxValidationResult toValidationResult() {
        return AviatorAdapter.SyntaxValidationResult.error(getMessage(), errorLine, errorColumn);
    }

    private static int[] extractPosition(String message) {
        if (message == null) return new int[]{-1, -1};
        Matcher m = POSITION_PATTERN.matcher(message);
        if (m.find()) {
            try {
                return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
            } catch (NumberFormatException ignored) {}
        }
        return new int[]{-1, -1};
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FormulaCompileException{message='")
            .append(getMessage()).append("'");
        if (hasPosition()) sb.append(", position=(").append(errorLine).append(":").append(errorColumn).append(")");
        return sb.append("}").toString();
    }
}