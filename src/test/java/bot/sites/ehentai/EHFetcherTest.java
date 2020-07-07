package bot.sites.ehentai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EHFetcherTest {
	private EHApiHandler handler = new EHApiHandler();

	@Test
	void checkMetadata() throws IOException, ExecutionException, InterruptedException {
		EHFetcher checker = new EHFetcher("https://e-hentai.org/g/1028286/37231df5ee/", handler);

		assertEquals(1028286, checker.getGalleryId());
		assertEquals("37231df5ee", checker.getGalleryToken());

		assertTrue(checker.getArtists().size() == 1 && String.join(", ", checker.getArtists()).equals("hisasi"));
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
	void checkMetadataPage() throws IOException, ExecutionException, InterruptedException {
		EHFetcher checker = new EHFetcher("https://e-hentai.org/s/75ec0bce98/1028286-16", handler);

		assertEquals(1028286, checker.getGalleryId());
		assertEquals("37231df5ee", checker.getGalleryToken());
	}
}
