package pro.sketchware.core.codegen;
import pro.sketchware.core.ComponentCodeGenerator;

/**
 * Formats generated Java and XML code with proper indentation.
 * Extracted from {@link ComponentCodeGenerator} to reduce its size.
 */
public class CodeFormatter {

    /**
     * @return Formatted code
     */
    public static String formatCode(String code, boolean indentMultiLineComments) {
        StringBuilder formattedCode = new StringBuilder(4096);
        char[] codeChars = code.toCharArray();
        boolean isXML = isXML(code);
        boolean processingSingleLineComment = false;
        boolean processingMultiLineComment = false;
        boolean processingEscape = false;
        int indentLevel = 0;
        boolean processingChar = false;
        boolean processingString = false;
        boolean isNewLine = true;

        for (int i = 0; i < codeChars.length; i++) {
            char codeBit = codeChars[i];

            if (isNewLine && !processingSingleLineComment && !processingMultiLineComment
                    && !processingChar && !processingString) {
                if (!isXML) {
                    if (codeBit == ' ' || codeBit == '\t') {
                        continue;
                    }
                    if (codeBit == '}') {
                        if (indentLevel > 0) {
                            appendIndent(formattedCode, indentLevel - 1);
                        }
                        formattedCode.append(codeBit);
                        if (indentLevel > 0) {
                            indentLevel -= 1;
                        }
                        if (i + 1 < codeChars.length && codeChars[i + 1] == ';') {
                            formattedCode.append(';');
                            i += 1;
                        }
                        isNewLine = false;
                        continue;
                    }
                    appendIndent(formattedCode, indentLevel);
                    isNewLine = false;
                }
            }

            if (processingSingleLineComment) {
                if (codeBit == '\n') {
                    formattedCode.append(codeBit);
                    isNewLine = true;
                    processingSingleLineComment = false;
                } else {
                    formattedCode.append(codeBit);
                }
            } else if (processingMultiLineComment) {
                if (codeBit == '*' && codeChars.length > i + 1 && codeChars[i + 1] == '/') {
                    formattedCode.append(codeBit).append(codeChars[i + 1]);
                    i += 1;
                    processingMultiLineComment = false;
                    continue;
                }
                formattedCode.append(codeBit);
                if (indentMultiLineComments && codeBit == '\n') {
                    isNewLine = true;
                    if (isXML) {
                        appendIndent(formattedCode, indentLevel);
                    }
                }
            } else if (processingEscape) {
                formattedCode.append(codeBit);
                processingEscape = false;
            } else if (codeBit == '\\') {
                formattedCode.append(codeBit);
                processingEscape = true;
            } else if (processingChar) {
                if (codeBit == '\'') {
                    formattedCode.append(codeBit);
                    processingChar = false;
                } else {
                    formattedCode.append(codeBit);
                }
            } else if (processingString) {
                if (codeBit == '"') {
                    formattedCode.append(codeBit);
                    processingString = false;
                } else {
                    formattedCode.append(codeBit);
                }
            } else {
                if (codeBit == '/' && codeChars.length > i + 1) {
                    char nextChar = codeChars[i + 1];
                    if (nextChar == '/') {
                        formattedCode.append(codeBit).append(nextChar);
                        i += 1;
                        processingSingleLineComment = true;
                        continue;
                    }
                    if (nextChar == '*') {
                        formattedCode.append(codeBit).append(nextChar);
                        i += 1;
                        processingMultiLineComment = true;
                        continue;
                    }
                }

                if (isXML) {
                    if (codeBit == '<' && codeChars.length > i + 1) {
                        char nextChar = codeChars[i + 1];
                        if (nextChar == '/') {
                            if (indentLevel > 0) {
                                indentLevel -= 1;
                            }
                            if (isNewLine) {
                                appendIndent(formattedCode, indentLevel);
                                isNewLine = false;
                            }
                            formattedCode.append(codeBit);
                        } else if (nextChar == '!' || nextChar == '?') {
                            if (isNewLine) {
                                appendIndent(formattedCode, indentLevel);
                                isNewLine = false;
                            }
                            formattedCode.append(codeBit);
                        } else {
                            if (isNewLine) {
                                appendIndent(formattedCode, indentLevel);
                                isNewLine = false;
                            }
                            formattedCode.append(codeBit);
                            indentLevel += 1;
                        }
                    } else if (codeBit == '>') {
                        formattedCode.append(codeBit);
                        if (i > 0 && codeChars[i - 1] == '/') {
                            if (indentLevel > 0) {
                                indentLevel -= 1;
                            }
                        }
                    } else if (codeBit == '\n') {
                        formattedCode.append(codeBit);
                        isNewLine = true;
                    } else {
                        if (isNewLine) {
                            appendIndent(formattedCode, indentLevel);
                            isNewLine = false;
                        }
                        formattedCode.append(codeBit);
                    }
                } else {
                    if (codeBit == '\n') {
                        formattedCode.append(codeBit);
                        isNewLine = true;
                    } else {
                        if (codeBit == '\'') {
                            processingChar = true;
                        }
                        if (codeBit == '"') {
                            processingString = true;
                        }
                        if (codeBit == '{') {
                            indentLevel += 1;
                            formattedCode.append(codeBit);
                            continue;
                        }
                        if (codeBit == '}') {
                            if (indentLevel > 0) {
                                indentLevel -= 1;
                            }
                            formattedCode.append(codeBit);
                            if (i + 1 < codeChars.length && codeChars[i + 1] == ';') {
                                formattedCode.append(';');
                                i += 1;
                            }
                            continue;
                        }
                        formattedCode.append(codeBit);
                    }
                }
            }
        }

        return formattedCode.toString();
    }

    private static boolean isXML(String code) {
        String trimmed = code.trim();
        return trimmed.startsWith("<?xml") || trimmed.startsWith("<") && trimmed.contains(">");
    }

    public static void appendIndent(StringBuilder builder, int indentSize) {
        for (int i = 0; i < indentSize; ++i) {
            builder.append('\t');
        }
    }
}
