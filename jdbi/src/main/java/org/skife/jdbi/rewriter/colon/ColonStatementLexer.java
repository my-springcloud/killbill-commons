/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

// $ANTLR 3.5.2 org/skife/jdbi/rewriter/colon/ColonStatementLexer.g 2020-04-24 07:20:39

    package org.skife.jdbi.rewriter.colon;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("all")
public class ColonStatementLexer extends Lexer {
	public static final int EOF=-1;
	public static final int COLON=4;
	public static final int DOUBLE_QUOTED_TEXT=5;
	public static final int ESCAPED_TEXT=6;
	public static final int ESCAPE_SEQUENCE=7;
	public static final int LITERAL=8;
	public static final int NAMED_PARAM=9;
	public static final int POSITIONAL_PARAM=10;
	public static final int QUOTED_TEXT=11;

	  @Override
	  public void reportError(RecognitionException e) {
	    throw new IllegalArgumentException(e);
	  }


	// delegates
	// delegators
	public Lexer[] getDelegates() {
		return new Lexer[] {};
	}

	public ColonStatementLexer() {} 
	public ColonStatementLexer(CharStream input) {
		this(input, new RecognizerSharedState());
	}
	public ColonStatementLexer(CharStream input, RecognizerSharedState state) {
		super(input,state);
	}
	@Override public String getGrammarFileName() { return "org/skife/jdbi/rewriter/colon/ColonStatementLexer.g"; }

	// $ANTLR start "LITERAL"
	public final void mLITERAL() throws RecognitionException {
		try {
			int _type = LITERAL;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:30:8: ( ( 'a' .. 'z' | 'A' .. 'Z' | ' ' | '\\t' | '\\n' | '\\r' | '0' .. '9' | ',' | '*' | '#' | '.' | '@' | '_' | '!' | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' | '%' | '&' | '^' | '|' | '$' | '~' | '{' | '}' | '`' )+ )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:30:10: ( 'a' .. 'z' | 'A' .. 'Z' | ' ' | '\\t' | '\\n' | '\\r' | '0' .. '9' | ',' | '*' | '#' | '.' | '@' | '_' | '!' | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' | '%' | '&' | '^' | '|' | '$' | '~' | '{' | '}' | '`' )+
			{
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:30:10: ( 'a' .. 'z' | 'A' .. 'Z' | ' ' | '\\t' | '\\n' | '\\r' | '0' .. '9' | ',' | '*' | '#' | '.' | '@' | '_' | '!' | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' | '%' | '&' | '^' | '|' | '$' | '~' | '{' | '}' | '`' )+
			int cnt1=0;
			loop1:
			while (true) {
				int alt1=2;
				int LA1_0 = input.LA(1);
				if ( ((LA1_0 >= '\t' && LA1_0 <= '\n')||LA1_0=='\r'||(LA1_0 >= ' ' && LA1_0 <= '!')||(LA1_0 >= '#' && LA1_0 <= '&')||(LA1_0 >= '(' && LA1_0 <= '9')||(LA1_0 >= ';' && LA1_0 <= '>')||(LA1_0 >= '@' && LA1_0 <= '[')||(LA1_0 >= ']' && LA1_0 <= '~')) ) {
					alt1=1;
				}

				switch (alt1) {
				case 1 :
					// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:
					{
					if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||input.LA(1)=='\r'||(input.LA(1) >= ' ' && input.LA(1) <= '!')||(input.LA(1) >= '#' && input.LA(1) <= '&')||(input.LA(1) >= '(' && input.LA(1) <= '9')||(input.LA(1) >= ';' && input.LA(1) <= '>')||(input.LA(1) >= '@' && input.LA(1) <= '[')||(input.LA(1) >= ']' && input.LA(1) <= '~') ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					if ( cnt1 >= 1 ) break loop1;
					EarlyExitException eee = new EarlyExitException(1, input);
					throw eee;
				}
				cnt1++;
			}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "LITERAL"

	// $ANTLR start "COLON"
	public final void mCOLON() throws RecognitionException {
		try {
			int _type = COLON;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:33:6: ( ':' )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:33:8: ':'
			{
			match(':'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "COLON"

	// $ANTLR start "NAMED_PARAM"
	public final void mNAMED_PARAM() throws RecognitionException {
		try {
			int _type = NAMED_PARAM;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:34:12: ( COLON ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '#' )+ )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:34:14: COLON ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '#' )+
			{
			mCOLON(); 

			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:34:20: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '#' )+
			int cnt2=0;
			loop2:
			while (true) {
				int alt2=2;
				int LA2_0 = input.LA(1);
				if ( (LA2_0=='#'||LA2_0=='.'||(LA2_0 >= '0' && LA2_0 <= '9')||(LA2_0 >= 'A' && LA2_0 <= 'Z')||LA2_0=='_'||(LA2_0 >= 'a' && LA2_0 <= 'z')) ) {
					alt2=1;
				}

				switch (alt2) {
				case 1 :
					// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:
					{
					if ( input.LA(1)=='#'||input.LA(1)=='.'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					if ( cnt2 >= 1 ) break loop2;
					EarlyExitException eee = new EarlyExitException(2, input);
					throw eee;
				}
				cnt2++;
			}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NAMED_PARAM"

	// $ANTLR start "POSITIONAL_PARAM"
	public final void mPOSITIONAL_PARAM() throws RecognitionException {
		try {
			int _type = POSITIONAL_PARAM;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:35:17: ( '?' )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:35:19: '?'
			{
			match('?'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "POSITIONAL_PARAM"

	// $ANTLR start "QUOTED_TEXT"
	public final void mQUOTED_TEXT() throws RecognitionException {
		try {
			int _type = QUOTED_TEXT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:36:12: ( ( '\\'' ( ESCAPE_SEQUENCE |~ '\\'' )* '\\'' ) )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:36:14: ( '\\'' ( ESCAPE_SEQUENCE |~ '\\'' )* '\\'' )
			{
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:36:14: ( '\\'' ( ESCAPE_SEQUENCE |~ '\\'' )* '\\'' )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:36:15: '\\'' ( ESCAPE_SEQUENCE |~ '\\'' )* '\\''
			{
			match('\''); 
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:36:20: ( ESCAPE_SEQUENCE |~ '\\'' )*
			loop3:
			while (true) {
				int alt3=3;
				int LA3_0 = input.LA(1);
				if ( (LA3_0=='\\') ) {
					int LA3_2 = input.LA(2);
					if ( (LA3_2=='\'') ) {
						int LA3_4 = input.LA(3);
						if ( ((LA3_4 >= '\u0000' && LA3_4 <= '\uFFFF')) ) {
							alt3=1;
						}
						else {
							alt3=2;
						}

					}
					else if ( ((LA3_2 >= '\u0000' && LA3_2 <= '&')||(LA3_2 >= '(' && LA3_2 <= '\uFFFF')) ) {
						alt3=2;
					}

				}
				else if ( ((LA3_0 >= '\u0000' && LA3_0 <= '&')||(LA3_0 >= '(' && LA3_0 <= '[')||(LA3_0 >= ']' && LA3_0 <= '\uFFFF')) ) {
					alt3=2;
				}

				switch (alt3) {
				case 1 :
					// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:36:22: ESCAPE_SEQUENCE
					{
					mESCAPE_SEQUENCE(); 

					}
					break;
				case 2 :
					// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:36:40: ~ '\\''
					{
					if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '&')||(input.LA(1) >= '(' && input.LA(1) <= '\uFFFF') ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					break loop3;
				}
			}

			match('\''); 
			}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "QUOTED_TEXT"

	// $ANTLR start "DOUBLE_QUOTED_TEXT"
	public final void mDOUBLE_QUOTED_TEXT() throws RecognitionException {
		try {
			int _type = DOUBLE_QUOTED_TEXT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:37:19: ( ( '\"' (~ '\"' )+ '\"' ) )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:37:21: ( '\"' (~ '\"' )+ '\"' )
			{
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:37:21: ( '\"' (~ '\"' )+ '\"' )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:37:22: '\"' (~ '\"' )+ '\"'
			{
			match('\"'); 
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:37:26: (~ '\"' )+
			int cnt4=0;
			loop4:
			while (true) {
				int alt4=2;
				int LA4_0 = input.LA(1);
				if ( ((LA4_0 >= '\u0000' && LA4_0 <= '!')||(LA4_0 >= '#' && LA4_0 <= '\uFFFF')) ) {
					alt4=1;
				}

				switch (alt4) {
				case 1 :
					// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:
					{
					if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '!')||(input.LA(1) >= '#' && input.LA(1) <= '\uFFFF') ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					if ( cnt4 >= 1 ) break loop4;
					EarlyExitException eee = new EarlyExitException(4, input);
					throw eee;
				}
				cnt4++;
			}

			match('\"'); 
			}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "DOUBLE_QUOTED_TEXT"

	// $ANTLR start "ESCAPED_TEXT"
	public final void mESCAPED_TEXT() throws RecognitionException {
		try {
			int _type = ESCAPED_TEXT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:38:14: ( '\\\\' . )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:38:16: '\\\\' .
			{
			match('\\'); 
			matchAny(); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "ESCAPED_TEXT"

	// $ANTLR start "ESCAPE_SEQUENCE"
	public final void mESCAPE_SEQUENCE() throws RecognitionException {
		try {
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:40:25: ( '\\\\' '\\'' )
			// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:40:29: '\\\\' '\\''
			{
			match('\\'); 
			match('\''); 
			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "ESCAPE_SEQUENCE"

	@Override
	public void mTokens() throws RecognitionException {
		// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:8: ( LITERAL | COLON | NAMED_PARAM | POSITIONAL_PARAM | QUOTED_TEXT | DOUBLE_QUOTED_TEXT | ESCAPED_TEXT )
		int alt5=7;
		switch ( input.LA(1) ) {
		case '\t':
		case '\n':
		case '\r':
		case ' ':
		case '!':
		case '#':
		case '$':
		case '%':
		case '&':
		case '(':
		case ')':
		case '*':
		case '+':
		case ',':
		case '-':
		case '.':
		case '/':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
		case ';':
		case '<':
		case '=':
		case '>':
		case '@':
		case 'A':
		case 'B':
		case 'C':
		case 'D':
		case 'E':
		case 'F':
		case 'G':
		case 'H':
		case 'I':
		case 'J':
		case 'K':
		case 'L':
		case 'M':
		case 'N':
		case 'O':
		case 'P':
		case 'Q':
		case 'R':
		case 'S':
		case 'T':
		case 'U':
		case 'V':
		case 'W':
		case 'X':
		case 'Y':
		case 'Z':
		case '[':
		case ']':
		case '^':
		case '_':
		case '`':
		case 'a':
		case 'b':
		case 'c':
		case 'd':
		case 'e':
		case 'f':
		case 'g':
		case 'h':
		case 'i':
		case 'j':
		case 'k':
		case 'l':
		case 'm':
		case 'n':
		case 'o':
		case 'p':
		case 'q':
		case 'r':
		case 's':
		case 't':
		case 'u':
		case 'v':
		case 'w':
		case 'x':
		case 'y':
		case 'z':
		case '{':
		case '|':
		case '}':
		case '~':
			{
			alt5=1;
			}
			break;
		case ':':
			{
			int LA5_2 = input.LA(2);
			if ( (LA5_2=='#'||LA5_2=='.'||(LA5_2 >= '0' && LA5_2 <= '9')||(LA5_2 >= 'A' && LA5_2 <= 'Z')||LA5_2=='_'||(LA5_2 >= 'a' && LA5_2 <= 'z')) ) {
				alt5=3;
			}

			else {
				alt5=2;
			}

			}
			break;
		case '?':
			{
			alt5=4;
			}
			break;
		case '\'':
			{
			alt5=5;
			}
			break;
		case '\"':
			{
			alt5=6;
			}
			break;
		case '\\':
			{
			alt5=7;
			}
			break;
		default:
			NoViableAltException nvae =
				new NoViableAltException("", 5, 0, input);
			throw nvae;
		}
		switch (alt5) {
			case 1 :
				// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:10: LITERAL
				{
				mLITERAL(); 

				}
				break;
			case 2 :
				// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:18: COLON
				{
				mCOLON(); 

				}
				break;
			case 3 :
				// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:24: NAMED_PARAM
				{
				mNAMED_PARAM(); 

				}
				break;
			case 4 :
				// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:36: POSITIONAL_PARAM
				{
				mPOSITIONAL_PARAM(); 

				}
				break;
			case 5 :
				// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:53: QUOTED_TEXT
				{
				mQUOTED_TEXT(); 

				}
				break;
			case 6 :
				// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:65: DOUBLE_QUOTED_TEXT
				{
				mDOUBLE_QUOTED_TEXT(); 

				}
				break;
			case 7 :
				// org/skife/jdbi/rewriter/colon/ColonStatementLexer.g:1:84: ESCAPED_TEXT
				{
				mESCAPED_TEXT(); 

				}
				break;

		}
	}



}
