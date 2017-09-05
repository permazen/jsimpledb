
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package io.permazen.spring;

import io.permazen.core.DeletedObjectException;
import io.permazen.core.InvalidSchemaException;
import io.permazen.core.ReferencedObjectException;
import io.permazen.core.RollbackOnlyTransactionException;
import io.permazen.core.SchemaMismatchException;
import io.permazen.core.StaleTransactionException;
import io.permazen.kv.RetryTransactionException;
import io.permazen.kv.TransactionTimeoutException;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * JSimpleDB implementation of Spring's {@link PersistenceExceptionTranslator} interface.
 *
 * @see io.permazen.spring
 */
@SuppressWarnings("serial")
public class JSimpleDBExceptionTranslator implements PersistenceExceptionTranslator {

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException e0) {
        if (e0 instanceof DeletedObjectException) {
            final DeletedObjectException e = (DeletedObjectException)e0;
            return new EmptyResultDataAccessException("object " + e.getId() + " not found", 1, e);
        }
        if (e0 instanceof InvalidSchemaException)
            return new InvalidDataAccessResourceUsageException(null, e0);
        if (e0 instanceof ReferencedObjectException)
            return new DataIntegrityViolationException(null, e0);
        if (e0 instanceof RollbackOnlyTransactionException)
            return new InvalidDataAccessApiUsageException(null, e0);
        if (e0 instanceof SchemaMismatchException)
            return new DataIntegrityViolationException(null, e0);
        if (e0 instanceof StaleTransactionException || e0 instanceof io.permazen.kv.StaleTransactionException)
            return new InvalidDataAccessApiUsageException(null, e0);
        if (e0 instanceof RetryTransactionException)
            return new ConcurrencyFailureException(null, e0);
        if (e0 instanceof TransactionTimeoutException)
            return new QueryTimeoutException(null, e0);
        return null;
    }
}

