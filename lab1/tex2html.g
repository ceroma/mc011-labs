grammar tex2html;

options {
  language = Python;
}

@header {
import os
from tex2htmlLexer import tex2htmlLexer
}

@main {
def main(argv, otherArg=None):
  char_stream = ANTLRFileStream(sys.argv[1])
  lexer = tex2htmlLexer(char_stream)
  tokens = CommonTokenStream(lexer)
  parser = tex2htmlParser(tokens)

  # Configure MathJax JS library:
  js_base = 'http://cdn.mathjax.org/mathjax/latest/MathJax.js'
  js_conf = '?config=TeX-AMS-MML_HTMLorMML'
  js_path = js_base + js_conf

  title, body = parser.result()
  print '<html>'
  print '<head>'
  print ' <title>' + title + '</title>'
  print ' <script type="text/javascript" src="' + js_path + '"></script>'
  print '</head>'
  print '<body>'
  print body
  print '</body>'
  print '</html>'
}

// Container for the title in "\title{...}":
@init {self.titleText = ''}

/*---------------------------------------------------------------------------*
 *                               Parser  Rules                               *
 *---------------------------------------------------------------------------*/

// Start rule:
result returns [result]
@init {result = ['', '']}
  : header* d=document {result = [self.titleText, d]}
  ;

// Header definitions, currently ignore everything except the title:
header
  : title
  | author
  | usepackage
  | documentclass
  | PARAGRAPH
  ;

// Title definition, save it for a "\maketitle" in the document area:
title
  : TITLE LEFT_CURLY w=words RIGHT_CURLY {self.titleText = w}
  ;

// These definitions are currently ignored, but are left as separate rules for
// completeness:
author
  : AUTHOR LEFT_CURLY words RIGHT_CURLY
  ;

usepackage
  : USEPACKAGE (LEFT_SQUARE WORD+ RIGHT_SQUARE)? LEFT_CURLY WORD+ RIGHT_CURLY
  ;

documentclass
  : DOCUMENTCLASS (LEFT_SQUARE WORD+ RIGHT_SQUARE)? LEFT_CURLY WORD+ RIGHT_CURLY
  ;

// Main area, between "\begin{document}" and "\end{document}":
document returns [body]
@init {body = []}
@after {body = ' '.join(body)}
  : BEG_DOCUMENT
    (c=content {body.append(c)})*
    END_DOCUMENT
  ;

// Content of the document can be a command, like "\maketitle" and
// "\begin{itemize}", math text or plain text:
content returns [content]
  : t=maketitle {content = t}
  | i=itemize {content = i}
  | i=image {content = i}
  | m=math {content = m}
  | t=text {content = t}
  ;

// "\maketitle" retrieves the previously saved title:
maketitle returns [title]
  : MAKETITLE {title = "<h1>" + self.titleText + "</h1>\n"}
  ;

// "\begin{itemize}" expects a list of "\item"s:
itemize returns [list]
@init {list = ''}
  : BEG_ITEMIZE {list += "<ul>\n"}
    (i=item {list += i})*
    END_ITEMIZE {list += "</ul>\n"}
  ;

// Everything after a "\item" will be rendered inside "<li>..</li>" tags, and
// can also be another unordered list, so any valid document content is
// accepted. Also, an optional description (between "[..]") will be rendered
// in bold at the beginning of the item:
item returns [item]
@init {item = []}
@after {item = "<li>" + ' '.join(item) + "</li>\n"}
  : ITEM
    (LEFT_SQUARE w=words RIGHT_SQUARE {item.append('<b>' + w + '</b>')})?
    (c=content {item.append(c)})+
  ;

// "\includegraphics{filename}" renders an "<img>" tag:
image returns [img]
  : IMAGE LEFT_CURLY w=WORD RIGHT_CURLY {img = '<img src="' + $w.text + '"/>'}
  ;

// Math text is surrounded by two "$" and accept brackets (escaped or not). It
// is rendered enclosed by "\(" and "\)", so it can be recognized by MathJax:
math returns [text]
@init {text = []}
@after {text = '\(' + ' '.join(text) + '\)'}
  : MATH_SIGN
    (
      w=(
        WORD | ESC_MATH_SIGN | ESC_LEFT_CURLY | ESC_RIGHT_CURLY
        | LEFT_CURLY | RIGHT_CURLY | LEFT_SQUARE | RIGHT_SQUARE
      )
      {text.append($w.text)}
    )+
    MATH_SIGN
  ;

// Plain text and it's modifiers (bold and italic). Note that we could bold an
// italic text, for example:
text returns [text]
  : BOLD LEFT_CURLY t=text RIGHT_CURLY {text = '<b>' + t + '</b>'}
  | ITALIC LEFT_CURLY t=text RIGHT_CURLY {text = '<i>' + t + '</i>'}
  | PARAGRAPH {text = "\n<br>\n"}
  | t=words {text = t}
  ;

// Sequence of words, rendered separated by a space. Also, a escaped "\$" should
// be shown as "$". Other reserved words that appear in text area should also be
// taken care of here (like "itemize" and "document"):
words returns [words]
@init {words = []}
@after {words = ' '.join(words)}
  : ( w=ESC_MATH_SIGN {words.append('$')}
    | w=ESC_LEFT_CURLY {words.append('{')}
    | w=ESC_RIGHT_CURLY {words.append('}')}
    | w=WORD {words.append($w.text)}
    )+
  ;

/*---------------------------------------------------------------------------*
 *                                Lexer Rules                                *
 *---------------------------------------------------------------------------*/

// Special tokens:
MATH_SIGN: '$';
LEFT_CURLY: '{';
RIGHT_CURLY: '}';
LEFT_SQUARE: '[';
RIGHT_SQUARE: ']';

// Escaped special tokens:
ESC_MATH_SIGN: BACK_SLASH MATH_SIGN;
ESC_LEFT_CURLY: BACK_SLASH LEFT_CURLY;
ESC_RIGHT_CURLY: BACK_SLASH RIGHT_CURLY;

// Header commands:
TITLE: BACK_SLASH 'title';
AUTHOR: BACK_SLASH 'author';
USEPACKAGE: BACK_SLASH 'usepackage';
DOCUMENTCLASS: BACK_SLASH 'documentclass';

// Begin/end commands and their options ("itemize" and "document"):
END_ITEMIZE: END SPACE* ITEMIZE;
BEG_ITEMIZE: BEGIN SPACE* ITEMIZE;
END_DOCUMENT: END SPACE* DOCUMENT;
BEG_DOCUMENT: BEGIN SPACE* DOCUMENT;
fragment ITEMIZE: '{itemize}';
fragment DOCUMENT: '{document}';
fragment END: BACK_SLASH 'end';
fragment BEGIN: BACK_SLASH 'begin';

// Document commands:
ITEM: BACK_SLASH 'item';
BOLD: BACK_SLASH 'textbf';
ITALIC: BACK_SLASH 'textit';
MAKETITLE: BACK_SLASH 'maketitle';
IMAGE: BACK_SLASH 'includegraphics';

// Word - anything except special tokens:
WORD: (LETTER | DIGIT | SYMBOL)+;
fragment LETTER: LOWER | UPPER;
fragment LOWER: 'a'..'z';
fragment UPPER: 'A'..'Z';
fragment DIGIT: '0'..'9';
fragment SYMBOL: '!'..'#' | '%'..'/' | ':'..'@' | '\\' | '^'..'`' | '|' | '~';
fragment BACK_SLASH: '\\';

// Ignore whitespaces:
WHITESPACE: SPACE+ {$channel = HIDDEN};
fragment SPACE: ' ' | '\t';

// A blank line should add some spacing between the texts, so recognizing
// two or more consecutive "new line" as a "paragraph". Single "new line"s
// are just ignored:
PARAGRAPH: NEW_LINE NEW_LINE+;
SINGLE_NEW_LINE: NEW_LINE {$channel = HIDDEN};
fragment NEW_LINE: '\r'? '\n';
