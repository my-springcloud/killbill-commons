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

// $ANTLR 3.5.2 org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g 2020-04-24 07:20:39

    package org.skife.jdbi.rewriter.printf;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("all")
public class FormatterStatementLexer extends Lexer {
	public static final int EOF=-1;
	public static final int INTEGER=4;
	public static final int LITERAL=5;
	public static final int QUOTED_TEXT=6;
	public static final int STRING=7;

	  @Override
	  public void reportError(RecognitionException e) {
	    throw new IllegalArgumentException(e);
	  }


	// delegates
	// delegators
	public Lexer[] getDelegates() {
		return new Lexer[] {};
	}

	public FormatterStatementLexer() {} 
	public FormatterStatementLexer(CharStream input) {
		this(input, new RecognizerSharedState());
	}
	public FormatterStatementLexer(CharStream input, RecognizerSharedState state) {
		super(input,state);
	}
	@Override public String getGrammarFileName() { return "org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g"; }

	// $ANTLR start "LITERAL"
	public final void mLITERAL() throws RecognitionException {
		try {
			int _type = LITERAL;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:30:8: ( ( 'a' .. 'z' | 'A' .. 'Z' | ' ' | '\\t' | '0' .. '9' | ',' | '*' | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' )+ )
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:30:10: ( 'a' .. 'z' | 'A' .. 'Z' | ' ' | '\\t' | '0' .. '9' | ',' | '*' | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' )+
			{
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:30:10: ( 'a' .. 'z' | 'A' .. 'Z' | ' ' | '\\t' | '0' .. '9' | ',' | '*' | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' )+
			int cnt1=0;
			loop1:
			while (true) {
				int alt1=2;
				int LA1_0 = input.LA(1);
				if ( (LA1_0=='\t'||LA1_0==' '||(LA1_0 >= '(' && LA1_0 <= '-')||(LA1_0 >= '/' && LA1_0 <= '9')||(LA1_0 >= ';' && LA1_0 <= '>')||(LA1_0 >= 'A' && LA1_0 <= '[')||LA1_0==']'||(LA1_0 >= 'a' && LA1_0 <= 'z')) ) {
					alt1=1;
				}

				switch (alt1) {
				case 1 :
					// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:
					{
					if ( input.LA(1)=='\t'||input.LA(1)==' '||(input.LA(1) >= '(' && input.LA(1) <= '-')||(input.LA(1) >= '/' && input.LA(1) <= '9')||(input.LA(1) >= ';' && input.LA(1) <= '>')||(input.LA(1) >= 'A' && input.LA(1) <= '[')||input.LA(1)==']'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
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

	// $ANTLR start "INTEGER"
	public final void mINTEGER() throws RecognitionException {
		try {
			int _type = INTEGER;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:32:8: ( '%d' )
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:32:10: '%d'
			{
			match("%d"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "INTEGER"

	// $ANTLR start "STRING"
	public final void mSTRING() throws RecognitionException {
		try {
			int _type = STRING;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:33:7: ( '%s' )
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:33:9: '%s'
			{
			match("%s"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "STRING"

	// $ANTLR start "QUOTED_TEXT"
	public final void mQUOTED_TEXT() throws RecognitionException {
		try {
			int _type = QUOTED_TEXT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:34:12: ( ( '\\'' (~ '\\'' )+ '\\'' ) )
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:34:14: ( '\\'' (~ '\\'' )+ '\\'' )
			{
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:34:14: ( '\\'' (~ '\\'' )+ '\\'' )
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:34:15: '\\'' (~ '\\'' )+ '\\''
			{
			match('\''); 
			// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:34:20: (~ '\\'' )+
			int cnt2=0;
			loop2:
			while (true) {
				int alt2=2;
				int LA2_0 = input.LA(1);
				if ( ((LA2_0 >= '\u0000' && LA2_0 <= '&')||(LA2_0 >= '(' && LA2_0 <= '\uFFFF')) ) {
					alt2=1;
				}

				switch (alt2) {
				case 1 :
					// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:
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
					if ( cnt2 >= 1 ) break loop2;
					EarlyExitException eee = new EarlyExitException(2, input);
					throw eee;
				}
				cnt2++;
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

	@Override
	public void mTokens() throws RecognitionException {
		// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:1:8: ( LITERAL | INTEGER | STRING | QUOTED_TEXT )
		int alt3=4;
		switch ( input.LA(1) ) {
		case '\t':
		case ' ':
		case '(':
		case ')':
		case '*':
		case '+':
		case ',':
		case '-':
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
			{
			alt3=1;
			}
			break;
		case '%':
			{
			int LA3_2 = input.LA(2);
			if ( (LA3_2=='d') ) {
				alt3=2;
			}
			else if ( (LA3_2=='s') ) {
				alt3=3;
			}

			else {
				int nvaeMark = input.mark();
				try {
					input.consume();
					NoViableAltException nvae =
						new NoViableAltException("", 3, 2, input);
					throw nvae;
				} finally {
					input.rewind(nvaeMark);
				}
			}

			}
			break;
		case '\'':
			{
			alt3=4;
			}
			break;
		default:
			NoViableAltException nvae =
				new NoViableAltException("", 3, 0, input);
			throw nvae;
		}
		switch (alt3) {
			case 1 :
				// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:1:10: LITERAL
				{
				mLITERAL(); 

				}
				break;
			case 2 :
				// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:1:18: INTEGER
				{
				mINTEGER(); 

				}
				break;
			case 3 :
				// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:1:26: STRING
				{
				mSTRING(); 

				}
				break;
			case 4 :
				// org/skife/jdbi/rewriter/printf/FormatterStatementLexer.g:1:33: QUOTED_TEXT
				{
				mQUOTED_TEXT(); 

				}
				break;

		}
	}



}
