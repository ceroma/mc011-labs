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
  : header* (c=content {html += c})*
  ;

header
  : author
  | title
  ;

author
  : BACK_SLASH AUTHOR LEFT_BRACK WORD+ RIGHT_BRACK
  ;

title
  : BACK_SLASH TITLE LEFT_BRACK w=words RIGHT_BRACK {self.titleText = w}
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
  : BACK_SLASH BOLD LEFT_BRACK t=text RIGHT_BRACK {text = '<b>' + t + '</b>'}
  | BACK_SLASH ITALIC LEFT_BRACK t=text RIGHT_BRACK {text = '<i>' + t + '</i>'}
  | MATH_SIGN t=words MATH_SIGN {text = "<div class=\"math\">" + t + "</div>"}
  | t=words {text = t }
  ;

itemize returns [command]
@init {command = ''}
  : BACK_SLASH BEGIN LEFT_BRACK ITEMIZE RIGHT_BRACK {command += "<ul>\n"}
    (i=item {command += i})*
    BACK_SLASH END LEFT_BRACK ITEMIZE RIGHT_BRACK {command += "</ul>\n"}
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
LEFT_BRACK: '{';
RIGHT_BRACK: '}';

AUTHOR: 'author';
TITLE: 'title';
MAKETITLE: 'maketitle';
BOLD: 'textbf';
ITALIC: 'textit';
BEGIN: 'begin';
END: 'end';
ITEMIZE: 'itemize';
ITEM: 'item';

WORD: (CHAR | DIGIT)+;
WHITESPACE: (' ' | '\t' | '\n')+ {$channel = HIDDEN};

fragment DIGIT: '0'..'9';
fragment CHAR: 'a'..'z' | 'A'..'Z';
