package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.fastily.jwiki.dwrap.PageSection;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class JWikiTree {

    JWikiTree parent;
    List<JWikiTree> children;

    PageSection content;

    public int getLevel() {
        return this.content.level;
    }

    public String getHeader() {
        return this.content.header;
    }

    public JWikiTree getLastChildren() {
        return children.isEmpty() ? null : children.get(children.size() - 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.content.level == -1) {
            sb.append("root");
        } else {
            sb.append(StringUtils.repeat('>', this.content.level)).append(" ").append(this.content.header);
        }
        if (!this.children.isEmpty()) {
            sb.append("\n");
            sb.append(this.children.stream().map(JWikiTree::toString).collect(Collectors.joining("\n")));
        }
        return sb.toString();
    }
}
