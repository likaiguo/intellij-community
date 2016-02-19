package org.jetbrains.yaml.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLQuotedText;

import java.util.ArrayList;
import java.util.List;

public class YAMLQuotedTextImpl extends YAMLScalarImpl implements YAMLQuotedText {
  private final boolean myIsSingleQuoted;

  public YAMLQuotedTextImpl(@NotNull ASTNode node) {
    super(node);
    myIsSingleQuoted = getNode().getFirstChildNode().getElementType() == YAMLTokenTypes.SCALAR_STRING;
  }

  @NotNull
  @Override
  public List<TextRange> getContentRanges() {
    List<TextRange> result = new ArrayList<TextRange>();

    final List<String> lines = StringUtil.split(getText(), "\n", true, false);
    // First line has opening quote
    int cumulativeOffset = 0;
    for (int i = 0; i < lines.size(); ++i) {
      final String line = lines.get(i);

      int lineStart = 0;
      int lineEnd = line.length();
      if (i == 0) {
        lineStart++;
      }
      else {
        while (lineStart < line.length() && Character.isWhitespace(line.charAt(lineStart))) {
          lineStart++;
        }
      }
      if (i == lines.size() - 1) {
        // Last line has closing quote
        lineEnd--;
      }
      else {
        while (lineEnd > lineStart && Character.isWhitespace(line.charAt(lineEnd - 1))) {
          lineEnd--;
        }
      }

      result.add(TextRange.create(lineStart, lineEnd).shiftRight(cumulativeOffset));
      cumulativeOffset += line.length() + 1;
    }

    return result;
  }

  @NotNull
  @Override
  protected String getRangesJoiner(@NotNull CharSequence leftString, @NotNull CharSequence rightString) {
    if (leftString.length() == 0 || !isSingleQuote() && leftString.charAt(leftString.length() - 1) == '\\') {
      return "\n";
    }
    else if (rightString.length() == 0) {
      return "";
    }
    else {
      return " ";
    }
  }

  @Override
  protected List<Pair<TextRange, String>> getDecodeReplacements(@NotNull CharSequence input) {
    List<Pair<TextRange, String>> result = new ArrayList<>();
    
    for (int i = 0; i + 1 < input.length(); ++i) {
      final CharSequence subSequence = input.subSequence(i, i + 2);
      final TextRange textRange = TextRange.create(i, i + 2);
      
      if (isSingleQuote() && "''".equals(subSequence)) {
        result.add(Pair.create(textRange, "'"));
      }
      else if (!isSingleQuote() && "\\\n".equals(subSequence)) {
        result.add(Pair.create(textRange, ""));
      }
      else if (!isSingleQuote() && "\\ ".equals(subSequence)) {
        result.add(Pair.create(textRange, " "));
      }
      else if (!isSingleQuote() && "\\\"".equals(subSequence)) {
        result.add(Pair.create(textRange, "\""));
      }
      else {
        i--;
      }
      i++;
    }
    return result;
  }

  @Override
  public boolean isMultiline() {
    return getText().contains("\n");
  }

  public boolean isSingleQuote() {
    return myIsSingleQuoted;
  }

  @Override
  public String toString() {
    return "YAML quoted text";
  }
}
