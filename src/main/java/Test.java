import bot.commands.Info;
import bot.modules.TagList;

public class Test {
	public static void main(String[] args) {
		System.out.println(Info.display(TagList.nonWholesomeTagsWithoutQuery("futanari")));
	}
}
