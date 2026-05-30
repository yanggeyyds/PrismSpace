package com.yzddmr6.prismspace.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class PseudoContentProviderTest {

	@Test public void baseProviderDoesNotRegisterItself() {
		assertFalse(new PseudoContentProvider().onCreate());
	}

	@Test public void resolverOperationsAreNoOp() {
		final PseudoContentProvider provider = new PseudoContentProvider();
		final Uri uri = mock(Uri.class);
		final ContentValues values = mock(ContentValues.class);

		final Cursor cursor = provider.query(uri, null, null, null, null);

		assertNull(cursor);
		assertNull(provider.getType(uri));
		assertNull(provider.insert(uri, values));
		assertEquals(0, provider.delete(uri, null, null));
		assertEquals(0, provider.update(uri, values, null, null));
	}
}
