package com.facebook.presto.sql.parser;

import com.facebook.presto.sql.tree.DateLiteral;
import com.facebook.presto.sql.tree.DoubleLiteral;
import com.facebook.presto.sql.tree.Expression;
import com.facebook.presto.sql.tree.IntervalLiteral;
import com.facebook.presto.sql.tree.IntervalLiteral.Sign;
import com.facebook.presto.sql.tree.Node;
import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.sql.tree.Query;
import com.facebook.presto.sql.tree.QuerySpecification;
import com.facebook.presto.sql.tree.SortItem;
import com.facebook.presto.sql.tree.Statement;
import com.facebook.presto.sql.tree.TimeLiteral;
import com.facebook.presto.sql.tree.With;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import static com.facebook.presto.sql.SqlFormatter.formatSql;
import static com.facebook.presto.sql.tree.QueryUtil.selectList;
import static com.facebook.presto.sql.tree.QueryUtil.table;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TestSqlParser
{
    @Test
    public void testDouble()
            throws Exception
    {
        assertExpression("123.", new DoubleLiteral("123"));
        assertExpression("123.0", new DoubleLiteral("123"));
        assertExpression(".5", new DoubleLiteral(".5"));
        assertExpression("123.5", new DoubleLiteral("123.5"));

        assertExpression("123E7", new DoubleLiteral("123E7"));
        assertExpression("123.E7", new DoubleLiteral("123E7"));
        assertExpression("123.0E7", new DoubleLiteral("123E7"));
        assertExpression("123E+7", new DoubleLiteral("123E7"));
        assertExpression("123E-7", new DoubleLiteral("123E-7"));

        assertExpression("123.456E7", new DoubleLiteral("123.456E7"));
        assertExpression("123.456E+7", new DoubleLiteral("123.456E7"));
        assertExpression("123.456E-7", new DoubleLiteral("123.456E-7"));

        assertExpression(".4E42", new DoubleLiteral(".4E42"));
        assertExpression(".4E+42", new DoubleLiteral(".4E42"));
        assertExpression(".4E-42", new DoubleLiteral(".4E-42"));
    }

    @Test
    public void testDoubleInQuery()
    {
        assertStatement("SELECT 123.456E7 FROM DUAL",
                new Query(
                        Optional.<With>absent(),
                        new QuerySpecification(
                                selectList(new DoubleLiteral("123.456E7")),
                                table(QualifiedName.of("DUAL")),
                                Optional.<Expression>absent(),
                                ImmutableList.<Expression>of(),
                                Optional.<Expression>absent(),
                                ImmutableList.<SortItem>of(),
                                Optional.<String>absent()),
                        ImmutableList.<SortItem>of(),
                        Optional.<String>absent()));
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:7: mismatched input 'x' expecting EOF")
    public void testExpressionWithTrailingJunk()
    {
        SqlParser.createExpression("1 + 1 x");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: no viable alternative at character '@'")
    public void testTokenizeErrorStartOfLine()
    {
        SqlParser.createStatement("@select");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:25: no viable alternative at character '@'")
    public void testTokenizeErrorMiddleOfLine()
    {
        SqlParser.createStatement("select * from foo where @what");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:20: mismatched character '<EOF>' expecting '''")
    public void testTokenizeErrorIncompleteToken()
    {
        SqlParser.createStatement("select * from 'oops");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 3:1: mismatched input 'from' expecting EOF")
    public void testParseErrorStartOfLine()
    {
        SqlParser.createStatement("select *\nfrom x\nfrom");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 3:7: no viable alternative at input 'from'")
    public void testParseErrorMiddleOfLine()
    {
        SqlParser.createStatement("select *\nfrom x\nwhere from");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:14: no viable alternative at input '<EOF>'")
    public void testParseErrorEndOfInput()
    {
        SqlParser.createStatement("select * from");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:16: no viable alternative at input '<EOF>'")
    public void testParseErrorEndOfInputWhitespace()
    {
        SqlParser.createStatement("select * from  ");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:15: backquoted identifiers are not supported; use double quotes to quote identifiers")
    public void testParseErrorBackquotes()
    {
        SqlParser.createStatement("select * from `foo`");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:19: backquoted identifiers are not supported; use double quotes to quote identifiers")
    public void testParseErrorBackquotesEndOfInput()
    {
        SqlParser.createStatement("select * from foo `bar`");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:8: identifiers must not start with a digit; surround the identifier with double quotes")
    public void testParseErrorDigitIdentifiers()
    {
        SqlParser.createStatement("select 1x from dual");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:35: no viable alternative at input 'order'")
    public void testParseErrorDualOrderBy()
    {
        SqlParser.createStatement("select fuu from dual order by fuu order by fuu");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:31: mismatched input 'order' expecting EOF")
    public void testParseErrorReverseOrderByLimit()
    {
        SqlParser.createStatement("select fuu from dual limit 10 order by fuu");
    }

    @Test
    public void testParsingExceptionPositionInfo()
    {
        try {
            SqlParser.createStatement("select *\nfrom x\nwhere from");
            fail("expected exception");
        }
        catch (ParsingException e) {
            assertEquals(e.getMessage(), "line 3:7: no viable alternative at input 'from'");
            assertEquals(e.getErrorMessage(), "no viable alternative at input 'from'");
            assertEquals(e.getLineNumber(), 3);
            assertEquals(e.getColumnNumber(), 7);
        }
    }

    @Test
    public void testInterval()
            throws Exception
    {
        assertExpression("INTERVAL '123' YEAR", new IntervalLiteral("123", "YEAR", Sign.POSITIVE));
        // assertExpression("INTERVAL '123-3' YEAR TO MONTH", new IntervalLiteral("123-3", "YEAR TO MONTH", Sign.POSITIVE));
        assertExpression("INTERVAL '123' MONTH", new IntervalLiteral("123", "MONTH", Sign.POSITIVE));
        assertExpression("INTERVAL '123' DAY", new IntervalLiteral("123", "DAY", Sign.POSITIVE));
        // assertExpression("INTERVAL '123 23:58:53.456' DAY TO SECOND", new IntervalLiteral("123 23:58:53.456", "DAY TO SECOND", Sign.POSITIVE));
        assertExpression("INTERVAL '123' HOUR", new IntervalLiteral("123", "HOUR", Sign.POSITIVE));
        // assertExpression("INTERVAL '23:59' HOUR TO MINUTE", new IntervalLiteral("23:58", "HOUR TO MINUTE", Sign.POSITIVE));
        assertExpression("INTERVAL '123' MINUTE", new IntervalLiteral("123", "MINUTE", Sign.POSITIVE));
        assertExpression("INTERVAL '123' SECOND", new IntervalLiteral("123", "SECOND", Sign.POSITIVE));
    }

    @Test
    public void testDate()
            throws Exception
    {
        assertExpression("DATE '2012-03-22'", new DateLiteral("2012-03-22"));
    }

    @Test
    public void testTime()
            throws Exception
    {
        assertExpression("TIME '03:04:05'", new TimeLiteral("03:04:05"));
    }

    private static void assertStatement(String query, Statement expected)
    {
        assertParsed(query, expected, SqlParser.createStatement(query));
    }

    private static void assertExpression(String expression, Expression expected)
    {
        assertParsed(expression, expected, SqlParser.createExpression(expression));
    }

    private static void assertParsed(String input, Node expected, Node parsed)
    {
        if (!parsed.equals(expected)) {
            fail(format("expected\n\n%s\n\nto parse as\n\n%s\n\nbut was\n\n%s\n",
                    indent(input),
                    indent(formatSql(expected)),
                    indent(formatSql(parsed))));
        }
    }

    private static String indent(String value)
    {
        String indent = "    ";
        return indent + value.trim().replaceAll("\n", "\n" + indent);
    }
}