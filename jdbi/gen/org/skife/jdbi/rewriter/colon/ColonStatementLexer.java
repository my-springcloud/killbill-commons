// Generated from C:/Users/zenglw/IdeaProjects/killbill-commons/jdbi/src/main/antlr3/org/skife/jdbi/rewriter/colon\ColonStatementLexer.g4 by ANTLR 4.8
package org.skife.jdbi.rewriter.colon;

    package org.skife.jdbi.rewriter.colon;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ColonStatementLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LITERAL=1, COLON=2, NAMED_PARAM=3, POSITIONAL_PARAM=4, QUOTED_TEXT=5, 
		DOUBLE_QUOTED_TEXT=6, ESCAPED_TEXT=7;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"LITERAL", "COLON", "NAMED_PARAM", "POSITIONAL_PARAM", "QUOTED_TEXT", 
			"DOUBLE_QUOTED_TEXT", "ESCAPED_TEXT", "ESCAPE_SEQUENCE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "':'", null, "'?'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LITERAL", "COLON", "NAMED_PARAM", "POSITIONAL_PARAM", "QUOTED_TEXT", 
			"DOUBLE_QUOTED_TEXT", "ESCAPED_TEXT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	  @Override
	  public void reportError(RecognitionException e) {
	    throw new IllegalArgumentException(e);
	  }


	public ColonStatementLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ColonStatementLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\t:\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\6\2\25\n\2"+
		"\r\2\16\2\26\3\3\3\3\3\4\3\4\6\4\35\n\4\r\4\16\4\36\3\5\3\5\3\6\3\6\3"+
		"\6\7\6&\n\6\f\6\16\6)\13\6\3\6\3\6\3\7\3\7\6\7/\n\7\r\7\16\7\60\3\7\3"+
		"\7\3\b\3\b\3\b\3\t\3\t\3\t\2\2\n\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\2\3"+
		"\2\6\n\2\13\f\17\17\"#%(*;=@B]_\u0080\b\2%%\60\60\62;C\\aac|\3\2))\3\2"+
		"$$\2=\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r"+
		"\3\2\2\2\2\17\3\2\2\2\3\24\3\2\2\2\5\30\3\2\2\2\7\32\3\2\2\2\t \3\2\2"+
		"\2\13\"\3\2\2\2\r,\3\2\2\2\17\64\3\2\2\2\21\67\3\2\2\2\23\25\t\2\2\2\24"+
		"\23\3\2\2\2\25\26\3\2\2\2\26\24\3\2\2\2\26\27\3\2\2\2\27\4\3\2\2\2\30"+
		"\31\7<\2\2\31\6\3\2\2\2\32\34\5\5\3\2\33\35\t\3\2\2\34\33\3\2\2\2\35\36"+
		"\3\2\2\2\36\34\3\2\2\2\36\37\3\2\2\2\37\b\3\2\2\2 !\7A\2\2!\n\3\2\2\2"+
		"\"\'\7)\2\2#&\5\21\t\2$&\n\4\2\2%#\3\2\2\2%$\3\2\2\2&)\3\2\2\2\'%\3\2"+
		"\2\2\'(\3\2\2\2(*\3\2\2\2)\'\3\2\2\2*+\7)\2\2+\f\3\2\2\2,.\7$\2\2-/\n"+
		"\5\2\2.-\3\2\2\2/\60\3\2\2\2\60.\3\2\2\2\60\61\3\2\2\2\61\62\3\2\2\2\62"+
		"\63\7$\2\2\63\16\3\2\2\2\64\65\7^\2\2\65\66\13\2\2\2\66\20\3\2\2\2\67"+
		"8\7^\2\289\7)\2\29\22\3\2\2\2\b\2\26\36%\'\60\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}