grammar WSL;

options { backtrack=true; memoize=true; }

@parser::header {
	package cc.warlock.core.stormfront.script.wsl;
	import java.util.ArrayList;
	import cc.warlock.core.stormfront.script.wsl.WSLEqualityCondition.EqualityOperator;
	import cc.warlock.core.stormfront.script.wsl.WSLRelationalCondition.RelationalOperator;
}

@lexer::header {
	package cc.warlock.core.stormfront.script.wsl;
}

@parser::members {
	private WSLScript script;
	private int lineNum = 1;
	public void setScript(WSLScript s) { script = s; }
	private boolean isNumber(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch(NumberFormatException e) {
			return false;
		}
	}
}

@lexer::members {
	private boolean atStart = true;	
}

script
	: line (EOL line)* EOF
	;

line
	: (label=LABEL)? c=expr
		{
			script.addCommand(c);
			if(label != null) {
				int existingLine = script.labelLineNumber($label.text);
				if(existingLine != -1)
					script.echo("Redefinition of label \"" + $label.text + "\" on line " + lineNum + ", originally defined on line " + existingLine);
				script.addLabel($label.text, c);
			}
			lineNum++;
		}
	;

expr returns [WSLAbstractCommand command]
	: (keyIF)=> keyIF cond=conditionalExpression keyTHEN c=expr
		{
			command = new WSLCondition(lineNum, script, cond, c);
		}
	| (keyACTION)=> (keyACTION c=expr keyWHEN args=string_list
		{
			command = new WSLAction(lineNum, script, c, args);
		}
	| keyACTION keyREMOVE args=string_list
		{
			command = new WSLActionRemove(lineNum, script, args);
		}
	| keyACTION keyCLEAR
		{
			command = new WSLActionClear(lineNum, script);
		})
	| args=string_list
		{
			command = new WSLCommand(lineNum, script, args);
		}
	| { command = new WSLCommand(lineNum, script, null); }
	;

string_list returns [IWSLValue value]
	: l=string_list_helper
			{
				if(l.size() > 1)
					value = new WSLList(l);
				else
					value = l.get(0);
			}
	;

string_list_helper returns [ArrayList<IWSLValue> list] @init { String whitespace = null; }
	: data=string_value
		{
			whitespace = "";
			for(int i = input.index() - 1; i >= 0 && input.get(i).getChannel() != Token.DEFAULT_CHANNEL; i--) {
				whitespace = input.get(i).getText() + whitespace;
			}
		}
	(l=string_list_helper)?
		{
			if(l == null) {
				list = new ArrayList<IWSLValue>();
				list.add(data);
			} else {
				list = l;
				if(whitespace != null && whitespace.length() > 0)
					list.add(0, new WSLString(whitespace));
				list.add(0, data);
			}
		}
	;

string_value returns [IWSLValue value]
	: v=STRING			{ value = new WSLString($v.text); }
	| v=VARIABLE		{ value = new WSLVariable($v.text, script); }
	| v=LOCAL_VARIABLE	{ value = new WSLLocalVariable($v.text, script); }
	| v=ESCAPED_CHAR	{ value = new WSLString($v.text); }
	| v=QUOTE			{ value = new WSLString($v.text); }
	;
	
conditionalExpression returns [IWSLValue cond] @init { ArrayList<IWSLValue> args = null; }
	: arg=conditionalAndExpression { args = null; }
		((keyOR)=> keyOR argNext=conditionalAndExpression
			{
				if(args == null) {
					args = new ArrayList<IWSLValue>();
					args.add(arg);
				}
				args.add(argNext);
			}
		)*
			{
				if(args == null)
					cond = arg;
				else
					cond = new WSLOrCondition(args);
			}
	;

conditionalAndExpression returns [IWSLValue cond] @init { ArrayList<IWSLValue> args = null; }
	: arg=equalityExpression
		((keyAND)=> keyAND argNext=equalityExpression
			{
				if(args == null) {
					args = new ArrayList<IWSLValue>();
					args.add(arg);
				}
				args.add(argNext);
			}
		)*
		{
			if(args == null)
				cond = arg;
			else
				cond = new WSLOrCondition(args);
		}
	;

equalityExpression returns [IWSLValue cond]
	@init {
			ArrayList<IWSLValue> args = null;
			ArrayList<EqualityOperator> ops = null;
		}
	: arg=relationalExpression { args = null; ops = null; }
		((equalityOp)=> op=equalityOp argNext=relationalExpression
			{
				if(args == null) {
					args = new ArrayList<IWSLValue>();
					args.add(arg);
				}
				args.add(argNext);
				if(ops == null) {
					ops = new ArrayList<EqualityOperator>();
				}
				ops.add(op);
			}
		)*
			{
				if(args == null)
					cond = arg;
				else
					cond = new WSLEqualityCondition(args, ops);
			}
	;

equalityOp returns [EqualityOperator op]
	: keyEQUAL			{ op = EqualityOperator.equals; }
	| keyNOTEQUAL		{ op = EqualityOperator.notequals; }
	;
	
relationalExpression returns [IWSLValue cond]
	@init {
			ArrayList<IWSLValue> args = null;
			ArrayList<RelationalOperator> ops = null;
		}
	: arg=unaryExpression { args = null; ops = null; }
		((relationalOp)=> op=relationalOp argNext=unaryExpression
			{
				if(args == null) {
					args = new ArrayList<IWSLValue>();
					args.add(arg);
				}
				args.add(argNext);
				if(ops == null) {
					ops = new ArrayList<RelationalOperator>();
				}
				ops.add(op);
			}
		)*
			{
				if(args == null)
					cond = arg;
				else
					cond = new WSLRelationalCondition(args, ops);
			}
	;

relationalOp returns [RelationalOperator op]
	: keyGT			{ op = RelationalOperator.GreaterThan; }
	| keyLT			{ op = RelationalOperator.LessThan; }
	| keyGTE		{ op = RelationalOperator.GreaterThanEqualTo; }
	| keyLTE		{ op = RelationalOperator.LessThanEqualTo; }
	| keyCONTAINS	{ op = RelationalOperator.Contains; }
	;

unaryExpression returns [IWSLValue cond]
	: keyNOT arg=unaryExpression	{ cond = new WSLNotCondition(arg); }
	| keyEXISTS arg=unaryExpression	{ cond = new WSLExistsCondition(arg); }
	| arg=primaryExpression			{ cond = arg; }
	;

parenExpression returns [IWSLValue cond]
	: keyLPAREN arg=conditionalExpression keyRPAREN		{ cond = arg; }
	;

primaryExpression returns [IWSLValue cond]
	: arg=parenExpression	{ cond = arg; }
	| (v=cond_value)		{ cond = v; }
	;
	
cond_value returns [IWSLValue value]
	: v=VARIABLE		{ value = new WSLVariable($v.text, script); }
	| v=LOCAL_VARIABLE	{ value = new WSLLocalVariable($v.text, script); }
	| (number)=> val=number		{ value = val; }
	| (keyTRUE)=> keyTRUE			{ value = new WSLBoolean(true); }
	| (keyFALSE)=> keyFALSE			{ value = new WSLBoolean(false); }
	| val=quoted_string	{ value = val; }
	;

number returns [IWSLValue value]
	: { isNumber(input.LT(1).getText()) }? v=STRING	{ value = new WSLNumber($v.text); }
	;

quoted_string returns [IWSLValue value]
	: QUOTE l=quoted_string_helper QUOTE
		{
			// FIXME: get preceding and following whitespace
			if(l.size() > 1)
				value = new WSLList(l);
			else
				value = l.get(0);
		}
	;

quoted_string_helper returns [ArrayList<IWSLValue> list] @init { String whitespace = null; }
	: data=quoted_string_value
		{
			whitespace = "";
			for(int i = input.index() - 1; i >= 0 && input.get(i).getChannel() != Token.DEFAULT_CHANNEL; i--) {
				whitespace = input.get(i).getText() + whitespace;
			}
		}
	(l=quoted_string_helper)?
		{
			if(l == null) {
				list = new ArrayList<IWSLValue>();
				list.add(data);
			} else {
				list = l;
				if(whitespace != null && whitespace.length() > 0)
					list.add(0, new WSLString(whitespace));
				list.add(0, data);
			}
		}
	;

quoted_string_value returns [IWSLValue value]
	: v=STRING			{ value = new WSLString($v.text); }
	| v=VARIABLE		{ value = new WSLVariable($v.text, script); }
	| v=LOCAL_VARIABLE	{ value = new WSLLocalVariable($v.text, script); }
	| v=ESCAPED_CHAR	{ value = new WSLString($v.text); }
	;

keyIF
	: { input.LT(1).getText().equalsIgnoreCase("if") }? STRING
	;
keyTHEN
	: { input.LT(1).getText().equalsIgnoreCase("then") }? STRING
	;
keyOR
	: { input.LT(1).getText().equalsIgnoreCase("or") || input.LT(1).getText().equals("||") }? STRING
	;
keyAND
	: { input.LT(1).getText().equalsIgnoreCase("and") || input.LT(1).getText().equals("&&") }? STRING
	;
keyNOT
	: { input.LT(1).getText().equalsIgnoreCase("not") || input.LT(1).getText().equals("!") }? STRING
	;
keyEQUAL
	: { input.LT(1).getText().equals("=") || input.LT(1).getText().equals("==") }? STRING
	;
keyNOTEQUAL
	: { input.LT(1).getText().equals("!=") || input.LT(1).getText().equals("<>") }? STRING
	;
keyGTE
	: { input.LT(1).getText().equals(">=") }? STRING
	;
keyLTE
	: { input.LT(1).getText().equals("<=") }? STRING
	;
keyGT
	: { input.LT(1).getText().equals(">") }? STRING
	;
keyLT
	: { input.LT(1).getText().equals("<") }? STRING
	;
keyLPAREN
	: { input.LT(1).getText().equals("(") }? STRING
	;
keyRPAREN
	: { input.LT(1).getText().equals(")") }? STRING
	;
keyEXISTS
	: { input.LT(1).getText().equalsIgnoreCase("exists") }? STRING
	;
keyCONTAINS
	: { input.LT(1).getText().equalsIgnoreCase("contains") || input.LT(1).getText().equalsIgnoreCase("indexof") }? STRING
	;
keyACTION
	: { input.LT(1).getText().equalsIgnoreCase("action") }? STRING
	;
keyWHEN
	: { input.LT(1).getText().equalsIgnoreCase("when") }? STRING
	;
keyREMOVE
	: { input.LT(1).getText().equalsIgnoreCase("remove") }? STRING
	;
keyCLEAR
	: { input.LT(1).getText().equalsIgnoreCase("clear") }? STRING
	;
keyTRUE
	: { input.LT(1).getText().equalsIgnoreCase("true") }? STRING
	;
keyFALSE
	: { input.LT(1).getText().equalsIgnoreCase("false") }? STRING
	;


BLANK
	: (' ' | '\t')+			{ $channel = HIDDEN; }
	;
EOL
	: '\r'? '\n' { atStart = true; }
	;
COMMENT
	: { atStart }?=>
		('#'|';') (~('\n'|'\r'))* { $channel = HIDDEN; }
	;
VARIABLE
	: '%' var=VARIABLE_STRING '%'?
			{
				setText($var.text);
				atStart = false;
			}
	;
LOCAL_VARIABLE
	:  '$' var=LOCAL_VARIABLE_STRING '$'?
			{
				setText($var.text);
				atStart = false;
			}
	;
STRING
	: ((~('%'|'$'|'\\'|'"'|WS))+ | '%' | '$') { atStart = false; }
	| ('%%'|'$$')
		{
			setText(getText().substring(0,1));
			atStart = false;
		}
	;
ESCAPED_CHAR
    : '\\' str=ANY { setText($str.text); atStart = false; }
	;
QUOTE
	: '"' { atStart = false; }
	;
LABEL
	: { atStart }?=> ( LABEL_STRING ':' )=> label=LABEL_STRING ':' { setText($label.text); atStart = false; }
	;

fragment WS
	: ' ' | '\t' | '\n' | '\r'
	;
fragment DIGIT
	: '0'..'9'
	;
fragment ANY
	: .
	;
fragment WORD_CHAR
	: ('a'..'z'|DIGIT|'_')
	;
fragment LABEL_STRING
	: WORD_CHAR (~(WS|':'))*
	;
fragment VARIABLE_STRING
	: WORD_CHAR (~(WS|'%'))*
	;
fragment LOCAL_VARIABLE_STRING
	: WORD_CHAR (~(WS|'$'))*
	; 
