package org.cryptomator.cryptofs;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.cryptolib.api.Cryptor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.cryptomator.cryptolib.Cryptors.cleartextSize;

@Module
class OpenCryptoFileModule {

	@Provides
	@PerOpenFile
	@CurrentOpenFilePath
	public AtomicReference<Path> provideCurrentPath(@OriginalOpenFilePath Path originalPath) {
		return new AtomicReference<>(originalPath);
	}

	@Provides
	@PerOpenFile
	public FileChannel provideFileChannel(EffectiveOpenOptions options, @OriginalOpenFilePath Path originalPath) {
		try {
			return originalPath.getFileSystem().provider().newFileChannel(originalPath, options.createOpenOptionsForEncryptedFile());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Provides
	@PerOpenFile
	public Supplier<BasicFileAttributeView> provideBasicFileAttributeViewSupplier(@CurrentOpenFilePath AtomicReference<Path> currentPath) {
		return () -> {
			Path path = currentPath.get();
			return path.getFileSystem().provider().getFileAttributeView(path, BasicFileAttributeView.class);
		};
	}

	@Provides
	@PerOpenFile
	@OpenFileSize
	public AtomicLong provideFileSize(FileChannel channel, Cryptor cryptor) {
		try {
			long size = channel.size();
			if (size == 0) {
				return new AtomicLong();
			} else {
				int headerSize = cryptor.fileHeaderCryptor().headerSize();
				return new AtomicLong(cleartextSize(size - headerSize, cryptor));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
