package nami.beitrag.letters;

import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import lombok.Getter;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class LetterGenerator {

    private static final String TEMPLATE_RECHNUNGEN = "rechnung.vm";
    private static final String PATH_TEMPLATES = LetterGenerator.class
            .getPackage().getName().replace(".", "/")
            + "/";

    private SqlSessionFactory sqlSessionFactory;

    static {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

    }

    public LetterGenerator(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    private static Template getTemplate(String templateName) {
        return Velocity.getTemplate(PATH_TEMPLATES + templateName);
    }

    @Getter
    public class RechnungRecipient {
        private BeitragMitglied mgl;
        private Collection<BeitragBuchung> buchungen;
    }

    public boolean generateRechnungen(LinkedHashMap<Integer, Collection<Integer>> rechnungen) {

        Template template = getTemplate(TEMPLATE_RECHNUNGEN);

        VelocityContext context = new VelocityContext();
        Collection<RechnungRecipient> recipients = new LinkedList<>();
        SqlSession session = sqlSessionFactory.openSession();
        BeitragMapper mapper = session.getMapper(BeitragMapper.class);
        for (Entry<Integer, Collection<Integer>> rechnung : rechnungen.entrySet()) {
            BeitragMitglied mgl = mapper.getMitglied(rechnung.getKey());
            Collection<BeitragBuchung> buchungen = new LinkedList<>(); 
            for (Integer buchungId : rechnung.getValue()) {
                buchungen.add(mapper.getBuchungById(buchungId));
            }

            RechnungRecipient rcpt = new RechnungRecipient();
            rcpt.mgl = mgl;
            rcpt.buchungen = buchungen;
            recipients.add(rcpt);
        }
        session.close();
        context.put("recipientList", recipients);

        StringWriter w = new StringWriter();
        template.merge(context, w);

        System.out.println(w);

        // TODO: richtige Rückgabe
        return false;
    }
}
