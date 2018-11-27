package com.expleague.sensearch.snippet;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.LogBasedMyStem;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import java.net.URI;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class RelevanceTest {

  private SnippetsCreator sc = new SnippetsCreator();
  private Index index = new Index() {
    @Override
    public Stream<Page> fetchDocuments(Query query) {
      return null;
    }

    @Override
    public Term[] synonyms(Term term) {
      return new Term[0];
    }

    @Override
    public boolean hasTitle(CharSequence title) {
      return false;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public int vocabularySize() {
      return 0;
    }

    @Override
    public double averagePageSize() {
      return 0;
    }

    @Override
    public int documentFrequency(Term term) {
      return 0;
    }

    @Override
    public long termFrequency(Term term) {
      return 0;
    }
  };

  private Lemmer lemmer;
  private Page d1, d2, d3;

  @Rule
  public TestName testName = new TestName();

  @Before
  public void prepare() {
    MyStem myStem = new LogBasedMyStem(
//          Paths.get("./resources/mystem"),
        Paths.get("myStemTestLogs", this.getClass().getName() + "_" + testName.getMethodName()));
    lemmer = new Lemmer(myStem);

    d1 = new Page() {
      @Override
      public URI reference() {
        return null;
      }

      @Override
      public CharSequence title() {
        return null;
      }

      @Override
      public CharSequence text() {
        return "Жил-был на свете добрый волшебник. "
            + "Синяя машины быстрее красной. "
            + "Он жил в мире, где магия стояла на первом месте и практически затмила собой науку. "
            + "Это десятый гол Роналду в карьере. "
            + "А этот волшебник (звали его Зак) очень увлекался астрономией. "
            + "На ноль делить не рекомендуется. "
            + "И вот однажды он подумал - а не сделать ли мне собственную звезду? "
            + "Девушка была необычайной красоты. "
            + "И сотворил звезду. И назвал её Солнце. А вокруг неё сделал несколько планет. "
            + "Сыроежка - мой любимый гриб. "
            + "А теперь он тёмными вечерами наблюдает за Солнцем и несколькими планетами. "
            + "Слон и муха - лучшие друзья. "
            + "Пьёт чай и тихонько улыбается. Потому что необыкновенно красиво. ";
      }
    };
    d2 = new Page() {
      @Override
      public URI reference() {
        return null;
      }

      @Override
      public CharSequence title() {
        return null;
      }

      @Override
      public CharSequence text() {
        return
            "Несущая смерть (оригинальное название Тамара ) — американский фильм ужасов 2005 года режиссёра Джереми Хэвта.\n"
                + "\n"
                + "Тамара Райли застенчивая и непривлекательная, но очень умная девушка, увлекается колдовством и тайно влюблена в своего преподавателя литературы Билла Натолли. Когда девушка пишет критическую статью об употреблении стероидов школьными спортсменами, Шон и Патрик, уличённые в содеянном, желают отомстить. Тамара пытается совершить магический ритуал, который помог бы ей навсегда связать свою судьбу с Биллом Натолли, но не решается его закончить. В ту же ночь Шон с Патриком решают её разыграть. Выдавая себя за Натолли, Шон приглашает Тамару в номер мотеля, в котором он спрятал камеру. Спрятавшись в душе, Шон предлагает прибывшей Тамаре раздеться. В соседнем номере Патрик, вместе с Хлоей, Джесси и Роджером (не знавшими о розыгрыше), наблюдает за происходящим по телевизору. Когда обман раскрылся, Тамара бросилась на обидчиков. В результате борьбы Тамара погибает. Несмотря на требование Хлои сообщить о произошедшем в полицию, друзья закапывают труп в лесу.\n"
                + "\n"
                + "На следующее утро все пятеро были шокированы тем, что Тамара, более привлекательная, чем ранее, пришла на занятия. Они убеждают себя, что девушка просто потеряла сознание, а очнувшись вылезла из ямы. В тот же вечер Тамара приходит к Роджеру в школьную видеостудию, где с помощью галлюцинаций показывает ему, что такое быть погребённым заживо. На следующее утро Роджер, после своего видеобращения к одноклассникам, совершает самоубийство.\n"
                + "\n"
                + "Затем Тамара приходит в дом мистера Натолли, намереваясь его соблазнить. Но он отвергает её, на что девушка отвечает, что «это лишь вопрос времени». На следующий день Тамара посещает жену Билла, школьного психолога Эллисон Натолли. В разговоре с ней Райли рассказывает о неспособности супругов зачать ребёнка. Тем же вечером Тамара внушает своему отцу съесть бутылки из под пива. На вечеринке она завораживает Патрика и Шона, заставляя их заниматься сексом друг с другом. Киша пытается остановить Тамару, но Тамара внушает, что она «кожа да кости», и ей нужно больше есть. Когда Хлоя и Джесси решают рассказать мистеру Натолли о произошедшем, Киша звонит Тамаре и сообщает место встречи. Хлоя ударом по лицу нокаутирует Кишу. Натолли, Хлоя и Джесси приезжают в дом Тамары, где обнаруживают труп её отца и книгу заклинаний, в которой описывается проведённый Тамарой ритуал. Они понимают, что пролив кровь Тамары они тем самым завершили ритуал, позволивший девушке восстать из могилы.\n"
                + "\n"
                + "Между тем, Тамара посылает Шона и Патрика убить Эллисон Натолли. Обороняясь, Эллисон приходится убить обоих. Вскоре в госпитале, куда были доставлены Киша и Эллисон, появляется Тамара. Она внушила Кише убить Хлою и Джесси, а сама стала преследовать Билла и Эллисон. На крыше больницы Хлоя пытается убить Тамару, но безрезультатно. Однако, Тамара не может прикосновением руки контролировать Хлою, так как та была невиновна в её гибели. В момент раскаяния Тамара начинает медленно разлагаться, но со словами «Тамара сдохла!» возвращается к прежнему облику. Тамара говорит Биллу, что их судьбы связаны до самой смерти. Натолли обнимает Тамару и, схватив её, бросается с крыши. В результате падения оба погибают. В конце Киша забирает книгу с приворотами из джипа.\n"
                + "\n"
                + "Дженна Дуан — Тамара Райли. Мэттью Марсден — Билл Натолли. Кэйти Стюарт — Хлоя. Клодетт Минк — Эллисон Натолли. Чед Фауст — Джесси. Мелисса Элиас — Киша. Гил Акоэн — Патрик. Крис Сигурдсон — отец Тамары.\n"
                + "\n"
                + "Картина вышла в прокат 3 февраля 2006 года. Фильм провалился в прокате, заработав при бюджете 3,5 миллиона всего 206,871 долларов.";
      }
    };

    d3 = new Page() {
      @Override
      public URI reference() {
        return null;
      }

      @Override
      public CharSequence title() {
        return null;
      }

      @Override
      public CharSequence text() {
        return "Xenodon pulcher — вид змей из семейства ужеобразных, обитающий в Южной Америке.\n"
            + "\n"
            + "Общая длина достигает 75 см. Шея слабо выражена, голова практически не отграничена от туловища. Ростральний щиток на конце морды увеличен и поднят кверху, как у свиноносых ужей. Туловище компактное, коренастое. По окраске напоминает королевских змей чередованием красных, чёрных и светлых колец, однако границы между полосами нечёткие, размытые. За светлым (ярко-белыми или лимонно-жёлтыми) полосами располагаются многочисленные мелкие чёрные крапинки. Изредка встречаются меланисты.\n"
            + "\n"
            + "Населяет окраины лесных массивов, редколесья, пампу. Ведёт наземный образ жизни. Активна днём. Питается земноводными, в частности лягушками. При угрозе сплющивает и выгибает переднюю треть туловища, совершая боковые броски.\n"
            + "\n"
            + "Это яйцекладущая змея. Самка откладывает до 7 яиц.\n"
            + "\n"
            + "Обитает в южной Бразилии, Боливии, Парагвае, северной Аргентине.\n";
      }
    };
  }


  @Test
  public void test() {
    Snippet s1 = sc.getSnippet(d1, new BaseQuery("волшебник", index, lemmer), lemmer);
    Snippet s2 = sc
        .getSnippet(d2, new BaseQuery("Несущая смерть Хлоя и Джесси", index, lemmer), lemmer);
    Snippet s3 = sc.getSnippet(d3, new BaseQuery("змея образ жизни", index, lemmer), lemmer);
    Assert.assertTrue(s1
        .getContent()
        .toString()
        .contains("..."));
    Assert.assertTrue(s2
        .getContent()
        .toString()
        .contains("..."));
    Assert.assertTrue(s3
        .getContent()
        .toString()
        .contains("..."));
  }
}

