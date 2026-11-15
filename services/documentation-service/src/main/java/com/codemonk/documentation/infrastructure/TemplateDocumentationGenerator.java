package com.codemonk.documentation.infrastructure;

import com.codemonk.documentation.application.DocumentationGenerator;
import com.codemonk.documentation.application.GeneratedDocumentation;
import com.codemonk.documentation.domain.DocType;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Default {@link DocumentationGenerator} that renders a markdown skeleton for the
 * requested documentation type.
 *
 * <p>It gives the service a working generator until analysis-backed generation
 * is available; replacing it only requires another {@link DocumentationGenerator}
 * bean.
 */
@Component
public class TemplateDocumentationGenerator implements DocumentationGenerator {

    @Override
    public GeneratedDocumentation generate(String repositoryId, DocType docType) {
        String title = displayName(docType) + " for " + repositoryId;
        String content = "# " + title + "\n\n"
                + "_Documentation type: " + docType.name() + "_\n\n"
                + "Content for this section has not been generated yet.\n";
        return new GeneratedDocumentation(title, content);
    }

    private static String displayName(DocType docType) {
        String[] words = docType.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }
}
