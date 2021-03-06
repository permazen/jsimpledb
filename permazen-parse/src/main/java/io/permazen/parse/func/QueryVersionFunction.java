
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package io.permazen.parse.func;

import io.permazen.JTransaction;
import io.permazen.core.CoreIndex;
import io.permazen.core.ObjId;
import io.permazen.core.ObjType;
import io.permazen.kv.KeyRanges;
import io.permazen.parse.ObjTypeParser;
import io.permazen.parse.ParseException;
import io.permazen.parse.ParseSession;
import io.permazen.parse.expr.AbstractValue;
import io.permazen.parse.expr.Node;
import io.permazen.parse.expr.Value;
import io.permazen.util.ParseContext;

public class QueryVersionFunction extends AbstractFunction {

    public QueryVersionFunction() {
        super("queryVersion");
    }

    @Override
    public String getHelpSummary() {
        return "Queries the database object version index";
    }

    @Override
    public String getUsage() {
        return "queryVersion([object-type])";
    }

    @Override
    public String getHelpDetail() {
        return "Queries the index of object versions, returning a map from object version to the set of objects with that version."
          + " An optional object type restricts returned objects.";
    }

    // Returns either null, ObjType, or Node
    @Override
    public Object parseParams(final ParseSession session, final ParseContext ctx, final boolean complete) {

        // Check existence of parameter
        if (ctx.tryLiteral(")"))
            return null;

        // Attempt to parse either type name or Java expression (hopefully evaluating to Class<?>)
        Object result;
        final int typeStart = ctx.getIndex();
        try {
            result = new ObjTypeParser().parse(session, ctx, complete);
            ctx.skipWhitespace();
            if (!ctx.tryLiteral(")"))
                throw new ParseException(ctx);
        } catch (ParseException e) {
            ctx.setIndex(typeStart);
            result = this.parseExpressionParams(session, ctx, complete, 0, 1, 1)[0];
        }

        // Done
        return result;
    }

    @Override
    public Value apply(ParseSession session, final Object param) {
        return new AbstractValue() {
            @Override
            public Object get(ParseSession session) {
                if (session.getMode().hasPermazen()) {
                    final Class<?> type =
                      param instanceof ObjType ?
                        session.getPermazen().getJClass(((ObjType)param).getStorageId()).getType() :
                      param instanceof Node ?
                        ((Node)param).evaluate(session).checkType(session, QueryVersionFunction.this.getName(), Class.class) :
                        Object.class;
                    return JTransaction.getCurrent().queryVersion(type);
                } else {
                    CoreIndex<Integer, ObjId> index = session.getTransaction().queryVersion();
                    final int storageId =
                      param instanceof Node ?
                        ((Node)param).evaluate(session).checkType(session, QueryVersionFunction.this.getName(), Integer.class) :
                      param instanceof ObjType ?
                        ((ObjType)param).getStorageId() :
                        -1;
                    if (storageId != -1)
                        index = index.filter(1, new KeyRanges(ObjId.getKeyRange(storageId)));
                    return index.asMap();
                }
            }
        };
    }
}

