package com.anthonyhilyard.merchantmarkers.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Just a simple re-implementation of JDK11's InputStream.nullInputStream().
 */
public class NullInputStream
{
	public static InputStream stream()
	{
		return new InputStream()
		{
			private volatile boolean closed;

			private void ensureOpen() throws IOException
			{
				if (closed)
				{
					throw new IOException("Stream closed");
				}
			}

			private int checkFromIndexSize(int fromIndex, int size, int len)
			{
				if ((len | fromIndex | size) < 0 || size > len - fromIndex)
				{
					throw new IndexOutOfBoundsException(String.format("Range [%d, %<d + %d) out of bounds for length %d", fromIndex, size, len));
				}
				return fromIndex;
			}

			@Override
			public int available () throws IOException
			{
				ensureOpen();
				return 0;
			}

			@Override
			public int read() throws IOException
			{
				ensureOpen();
				return -1;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException
			{
				checkFromIndexSize(off, len, b.length);
				if (len == 0)
				{
					return 0;
				}
				ensureOpen();
				return -1;
			}

			@Override
			public long skip(long n) throws IOException
			{
				ensureOpen();
				return 0L;
			}

			@Override
			public void close() throws IOException
			{
				closed = true;
			}
		};
	}
}
