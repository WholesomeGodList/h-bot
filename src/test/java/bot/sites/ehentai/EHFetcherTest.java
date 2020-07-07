package bot.sites.ehentai;

import org.jsoup.HttpStatusException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EHFetcherTest {
	@Test
	void checkMetadata() throws IOException {
		EHFetcher checker = new EHFetcher("https://e-hentai.org/g/1028286/37231df5ee/");

		assertEquals(1028286, checker.getGalleryId());
		assertEquals("37231df5ee", checker.getGalleryToken());

		assertTrue(checker.getArtists().size() == 1 && checker.getArtists().get(0).equals("hisasi"));
		assertEquals(1, checker.getParodies().size());
		assertEquals("shokugeki no soma", checker.getParodies().get(0));
		assertEquals(1, checker.getMaleTags().size());
		assertTrue(checker.getMaleTags().contains("sole male"));
		assertEquals(5, checker.getFemaleTags().size());
		assertTrue(checker.getFemaleTags().contains("ahegao"));
		assertEquals(2, checker.getMiscTags().size());
		assertTrue(checker.getMiscTags().contains("full color"));
		assertEquals(1, checker.getGroups().size());
		assertEquals("neko wa manma ga utsukushii", checker.getGroups().get(0));

		assertEquals("Erina-sama's Love Laboratory. 2", checker.getTitle());
		assertEquals("Armada Du", checker.getUploader());
		assertEquals("English", checker.getLanguage());
		assertEquals(25, checker.getPages());

		assertEquals(checker.getCategory(), EHFetcher.Category.DOUJINSHI);
	}

	@Test
	void checkMetadataPage() throws IOException {
		EHFetcher checker = new EHFetcher("https://e-hentai.org/s/75ec0bce98/1028286-16");

		assertEquals(1028286, checker.getGalleryId());
		assertEquals("37231df5ee", checker.getGalleryToken());
	}

	@Test
	void checkRegex() {
		assertTrue(regexCheckUtil("htasodfiajsdof"));
		assertTrue(regexCheckUtil("https://exhentai.org/g/69420/NotStonks"));
		assertTrue(regexCheckUtil("https://exhentai.org/\\//////"));
		assertTrue(regexCheckUtil("https://exhentai.org/g/2342304/aaaaaaaaaaa/"));
		assertTrue(regexCheckUtil("https://exhentai.org/s/2342304/aaaaaaaaaaa/"));
		assertTrue(regexCheckUtil("https://e-hentai.org/s/1028286/37231df5ee/"));
		assertTrue(regexCheckUtil("https://e-hentai.org/g/75ec0bce98/1028286-16"));
	}

	boolean regexCheckUtil(String test) {
		try {
			new EHFetcher(test);
		} catch (Exception e) {
			return e instanceof HttpStatusException && ((HttpStatusException) e).getStatusCode() == 404 && e.getMessage().equals("Not proper link");
		}
		return false;
	}
}
