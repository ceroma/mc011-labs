grammar tex2html;

options {
  language = Python;
}

@header {
from tex2htmlLexer import tex2htmlLexer
}

@main {
def main(argv, otherArg=None):
  char_stream = ANTLRFileStream(sys.argv[1])
  lexer = tex2htmlLexer(char_stream)
  tokens = CommonTokenStream(lexer)
  parser = tex2htmlParser(tokens)

  print parser.result()
}

@init {self.titleText = ''}

result returns [html]
@init {html = ''}
  : header* d=document {html = d}
  ;

header
  : author
  | title
  | usepackage
  | documentclass
  ;

author
  : BACK_SLASH AUTHOR LEFT_CURLY WORD+ RIGHT_CURLY
  ;

title
  : BACK_SLASH TITLE LEFT_CURLY w=words RIGHT_CURLY {self.titleText = w}
  ;

usepackage
  : BACK_SLASH USEPACKAGE (LEFT_SQUARE WORD+ RIGHT_SQUARE)? LEFT_CURLY WORD+ RIGHT_CURLY
  ;

documentclass
  : BACK_SLASH DOCUMENTCLASS (LEFT_SQUARE WORD+ RIGHT_SQUARE)? LEFT_CURLY WORD+ RIGHT_CURLY
  ;

document returns [body]
@init {body = ''}
  : BACK_SLASH BEGIN LEFT_CURLY DOCUMENT RIGHT_CURLY
    (c=content {body += c})*
    BACK_SLASH END LEFT_CURLY DOCUMENT RIGHT_CURLY
  ;

content returns [content]
  : t=maketitle {content = t}
  | i=itemize {content = i}
  | t=text {content = t}
  ;

maketitle returns [title]
  : BACK_SLASH MAKETITLE {title = "<h1>" + self.titleText + "</h1>\n"}
  ;

text returns [text]
  : BACK_SLASH BOLD LEFT_CURLY t=text RIGHT_CURLY {text = '<b>' + t + '</b>'}
  | BACK_SLASH ITALIC LEFT_CURLY t=text RIGHT_CURLY {text = '<i>' + t + '</i>'}
  | MATH_SIGN t=words MATH_SIGN {text = "<div class=\"math\">" + t + "</div>"}
  | t=words {text = t }
  ;

itemize returns [command]
@init {command = ''}
  : BACK_SLASH BEGIN LEFT_CURLY ITEMIZE RIGHT_CURLY {command += "<ul>\n"}
    (i=item {command += i})*
    BACK_SLASH END LEFT_CURLY ITEMIZE RIGHT_CURLY {command += "</ul>\n"}
  ;
    
item returns [item]
@init {item = []}
@after {item = "<li>" + ' '.join(item) + "</li>\n"}
  : BACK_SLASH ITEM (c=content {item.append(c)})+
  ;

words returns [words]
@init {words = []}
@after {words = ' '.join(words)}
  : (w1=WORD {words.append($w1.text)})+
  ;

BACK_SLASH: '\\';
MATH_SIGN: '$';
LEFT_CURLY: '{';
RIGHT_CURLY: '}';
LEFT_SQUARE: '[';
RIGHT_SQUARE: ']';

AUTHOR: 'author';
TITLE: 'title';
DOCUMENT: 'document';
MAKETITLE: 'maketitle';
USEPACKAGE: 'usepackage';
DOCUMENTCLASS: 'documentclass';
BOLD: 'textbf';
ITALIC: 'textit';
BEGIN: 'begin';
END: 'end';
ITEMIZE: 'itemize';
ITEM: 'item';

WORD: (CHAR | DIGIT | SYMB)+;
WHITESPACE: (' ' | '\t' | '\n')+ {$channel = HIDDEN};

fragment DIGIT: '0'..'9';
fragment CHAR: 'a'..'z' | 'A'..'Z';
fragment SYMB: '-' | ',' | ':' | '.';
