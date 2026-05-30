package com.yzddmr6.prismspace.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScopesTest {

	@Test public void processScopeTracksMarkMarkOnlyAndUnmark() {
		final Scopes.Scope scope = Scopes.process();
		final String tag = "scopes-test-" + System.nanoTime();

		assertFalse(scope.isMarked(tag));
		assertTrue(scope.mark(tag));
		assertTrue(scope.isMarked(tag));
		assertFalse(scope.mark(tag));

		assertTrue(scope.unmark(tag));
		assertFalse(scope.isMarked(tag));
		assertFalse(scope.unmark(tag));

		scope.markOnly(tag);
		assertTrue(scope.isMarked(tag));
		assertTrue(scope.unmark(tag));
	}

	@Test public void scopedTagDelegatesToScope() {
		final Scopes.Scope scope = Scopes.process();
		final String tag = "scoped-tag-test-" + System.nanoTime();
		final Scopes.ScopedTag scoped = scope.tag(tag);

		assertFalse(scoped.isMarked());
		scoped.markOnly();
		assertTrue(scoped.isMarked());
		assertTrue(scoped.unmark());
		assertFalse(scoped.isMarked());
	}
}
