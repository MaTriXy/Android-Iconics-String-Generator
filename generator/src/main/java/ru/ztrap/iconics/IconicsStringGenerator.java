package ru.ztrap.iconics;

import com.mikepenz.iconics.typeface.ITypeface;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * @author pa.gulko zTrap (29.03.2018)
 */
public abstract class IconicsStringGenerator {
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("(?=\\p{Lu})");
    private static final String WORD_DELIMITER = "_";
    private static final String XML = "xml";

    protected enum FileCreationStrategy {
        SAVE_OLD, SAVE_ONLY_CURRENT
    }

    /**
     * @return modifier for mark file as current-version file
     * */
    protected String modifierCurrent() {
        return "_current_";
    }

    /**
     * @return directory path for generated .xml file
     * */
    protected String outputDirectory() {
        return "src" + File.separator + "main" + File.separator + "res" + File.separator + "values";
    }

    /**
     * Define resolution strategy for new versions
     * */
    protected abstract FileCreationStrategy fileCreationStrategy();

    /**
     * Magic live here
     * */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void generateIconsFrom(ITypeface typeface) throws ParserConfigurationException, TransformerException {
        assert fileCreationStrategy() != null;
        assert typeface != null;

        final String handledClassName = handleWords(typeface.getFontName()) + "_v";

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();

        Element resources = doc.createElement("resources");
        Comment comment1 = doc.createComment(" Generated by Android-Iconics String Generator v"
                + BuildConfig.VERSION_NAME
                + " at "
                + new Date()
                + " ");
        Comment comment2 = doc.createComment(" https://github.com/zTrap/Android-Iconics-String-Generator ");

        doc.appendChild(resources);
        resources.appendChild(comment1);
        resources.appendChild(comment2);

        for (String icon : typeface.getIcons()) {
            Element iconElement = doc.createElement("string");
            iconElement.setAttribute("name", icon);
            iconElement.setTextContent(icon);
            resources.appendChild(iconElement);
        }

        String fileName = handledClassName + typeface.getVersion() + "." + XML;
        File fontDirectory = new File(outputDirectory());

        File newFile = new File(fontDirectory, modifierCurrent() + fileName);

        if (newFile.exists()) {
            // replace file's content with same version
            newFile.delete();
        }

        // search actual file with leading #modifierCurrent()
        File[] files = fontDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().matches(
                        modifierCurrent()
                                + handledClassName
                                + ".+\\."
                                + XML
                );
            }
        });


        if (files != null && files.length > 0){
            File current = files[0];
                switch (fileCreationStrategy()) {
                    case SAVE_OLD:
                        File renamed = new File(fontDirectory, current.getName().replace(modifierCurrent(), ""));
                        if (!current.renameTo(renamed)) {
                            String message;
                            if (renamed.exists()) {
                                message = String.format(
                                        "Unable to rename file from %1$s to %2$s. File %2$s is already exist.",
                                        current.getName(), renamed.getName());
                            } else {
                                message = String.format(
                                        "Unable to rename file from %1$s to %2$s.",
                                        current.getName(), renamed.getName());
                            }
                            throw new IllegalArgumentException(message);
                        }
                        break;
                    case SAVE_ONLY_CURRENT:
                        current.delete();
                        break;
                }
        }
        //endregion

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc), new StreamResult(newFile));
    }

    protected String handleWords(String fieldName) {
        fieldName = fieldName.replace(" ", "");
        String[] words = UPPERCASE_PATTERN.split(fieldName);
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(WORD_DELIMITER);
            }
            sb.append(word.toLowerCase());
        }
        return sb.toString();
    }
}