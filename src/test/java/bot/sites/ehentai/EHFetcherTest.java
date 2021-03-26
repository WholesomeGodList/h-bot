package bot.sites.ehentai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EHFetcherTest {
	private final EHApiHandler handler = new EHApiHandler();
	private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

	@Test
	void checkMetadata() throws IOException, ExecutionException, InterruptedException {
		executor.setCorePoolSize(10);

		CompletableFuture<EHFetcher> test = new CompletableFuture<>();
		for(int i = 0; i < 9; i++) {
			executor.submit(
					() -> {
						try {
							EHFetcher cur = new EHFetcher("https://e-hentai.org/g/1028286/37231df5ee/", handler, null);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
			);
		}

		executor.submit(
				() -> {
					try {
						EHFetcher cur = new EHFetcher("https://e-hentai.org/g/1028286/37231df5ee/", handler, null);
						test.complete(cur);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		);

		EHFetcher checker = test.get();

		assertEquals(1028286, checker.getGalleryId());
		assertEquals("37231df5ee", checker.getGalleryToken());

		assertTrue(checker.getArtists().size() == 1 && String.join(", ", checker.getArtists()).equals("Hisasi"));
		assertEquals(1, checker.getParodies().size());
		assertEquals("shokugeki no soma", String.join(", ", checker.getParodies()));
		assertEquals(1, checker.getMaleTags().size());
		assertTrue(checker.getMaleTags().contains("sole male"));
		assertEquals(5, checker.getFemaleTags().size());
		assertTrue(checker.getFemaleTags().contains("ahegao"));
		assertEquals(2, checker.getMiscTags().size());
		assertTrue(checker.getMiscTags().contains("full color"));
		assertEquals(1, checker.getGroups().size());
		assertEquals("neko wa manma ga utsukushii", String.join(", ", checker.getGroups()));

		assertEquals("Erina-sama's Love Laboratory. 2", checker.getTitle());
		assertEquals("Armada Du", checker.getUploader());
		assertEquals("English", checker.getLanguage());
		assertEquals(25, checker.getPages());

		assertEquals(checker.getCategory(), EHFetcher.Category.DOUJINSHI);


	}

	@Test
	void checkMetadataPage() throws IOException {
		EHFetcher checker = new EHFetcher("https://e-hentai.org/s/75ec0bce98/1028286-16/", handler, null);

		assertEquals(1028286, checker.getGalleryId());
		assertEquals("37231df5ee", checker.getGalleryToken());
	}
}
