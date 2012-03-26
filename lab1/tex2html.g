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

  # Find the absolute path of jsMath library:
  src_path = os.path.dirname(os.path.realpath(__file__))
  js_path = os.path.join(src_path, 'jsMath', 'easy', 'load.js')

  title, body = parser.result()
  print '<head>'
  print '  <title>' + title + '</title>'
  print '  <script src="' + js_path + '"></script>'
  print '</head>'
  print '<body>'
  print body
  print '</body>'
}

@init {self.titleText = ''}

result returns [result]
@init {result = ['', '']}
  : header* d=document {result = [self.titleText, d]}
  ;

header
  : author
  | title
  | usepackage
  | documentclass
  ;

author
  : AUTHOR LEFT_CURLY WORD+ RIGHT_CURLY
  ;

title
  : TITLE LEFT_CURLY w=words RIGHT_CURLY {self.titleText = w}
  ;

usepackage
  : USEPACKAGE (LEFT_SQUARE WORD+ RIGHT_SQUARE)? LEFT_CURLY WORD+ RIGHT_CURLY
  ;

documentclass
  : DOCUMENTCLASS (LEFT_SQUARE WORD+ RIGHT_SQUARE)? LEFT_CURLY WORD+ RIGHT_CURLY
  ;

document returns [body]
@init {body = []}
@after {body = ' '.join(body)}
  : BEGIN LEFT_CURLY DOCUMENT RIGHT_CURLY
    (c=content {body.append(c)})*
    END LEFT_CURLY DOCUMENT RIGHT_CURLY
  ;

content returns [content]
  : t=maketitle {content = t}
  | i=itemize {content = i}
  | t=text {content = t}
  ;

maketitle returns [title]
  : MAKETITLE {title = "<h1>" + self.titleText + "</h1>\n"}
  ;

text returns [text]
  : BOLD LEFT_CURLY t=text RIGHT_CURLY {text = '<b>' + t + '</b>'}
  | ITALIC LEFT_CURLY t=text RIGHT_CURLY {text = '<i>' + t + '</i>'}
  | IMAGE LEFT_CURLY t=text RIGHT_CURLY {text = '<img src="' + t + '"/>'}
  | MATH_SIGN t=words MATH_SIGN {text = '\(' + t + '\)'}
  | t=words {text = t}
  ;

itemize returns [command]
@init {command = ''}
  : BEGIN LEFT_CURLY ITEMIZE RIGHT_CURLY {command += "<ul>\n"}
    (i=item {command += i})*
    END LEFT_CURLY ITEMIZE RIGHT_CURLY {command += "</ul>\n"}
  ;
    
item returns [item]
@init {item = []}
@after {item = "<li>" + ' '.join(item) + "</li>\n"}
  : ITEM (c=content {item.append(c)})+
  ;

words returns [words]
@init {words = []}
@after {words = ' '.join(words)}
  : (w1=WORD {words.append($w1.text)})+
  ;

MATH_SIGN: '$';
LEFT_CURLY: '{';
RIGHT_CURLY: '}';
LEFT_SQUARE: '[';
RIGHT_SQUARE: ']';

TITLE: BACK_SLASH 'title';
AUTHOR: BACK_SLASH 'author';
USEPACKAGE: BACK_SLASH 'usepackage';
DOCUMENTCLASS: BACK_SLASH 'documentclass';

ITEMIZE: 'itemize';
DOCUMENT: 'document';
END: BACK_SLASH 'end';
BEGIN: BACK_SLASH 'begin';

ITEM: BACK_SLASH 'item';
BOLD: BACK_SLASH 'textbf';
ITALIC: BACK_SLASH 'textit';
MAKETITLE: BACK_SLASH 'maketitle';
IMAGE: BACK_SLASH 'includegraphics';

WORD: (CHAR | DIGIT | SYMB)+;
WHITESPACE: (' ' | '\t' | '\n')+ {$channel = HIDDEN};

fragment DIGIT: '0'..'9';
fragment CHAR: 'a'..'z' | 'A'..'Z';
fragment SYMB: '-' | ',' | ':' | '.' | '^' | '+' | '=' | '_' | '\\' | '(' | ')';
fragment BACK_SLASH: '\\';
