package pyguy.types;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class HelpTextParagraph extends JTextPane
{
    // CONSTANTS
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\[\\]]+)]\\(([^()]+)\\)");

    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);

    private static final Color LINK_COLOR_LIGHT = new Color(0, 0, 238);
    private static final Color LINK_COLOR_DARK  = new Color(153, 195, 255);

    // STATE
    private static final SimpleAttributeSet linkSet      = new SimpleAttributeSet();
    private static final SimpleAttributeSet linkHoverSet = new SimpleAttributeSet();

    private final List<Pair<Integer, Integer>> linkRanges = new ArrayList<>();
    private final List<URI>                    links      = new ArrayList<>();

    private int linkHovered = -1;

    static
    {
        StyleConstants.setUnderline(linkSet, false);

        StyleConstants.setUnderline(linkHoverSet, true);
    }

    private void SetStyle(Pair<Integer, Integer> range, AttributeSet attributeSet)
    {
        getStyledDocument().setCharacterAttributes(
            range.a(),
            range.b() - range.a(),
            attributeSet,
            false
        );
    }

    private void HoverLink(int link, boolean hover)
    {
        if (hover)
        {
            setCursor(HAND_CURSOR);
            setToolTipText(links.get(link).toString());
        }
        else
        {
            setCursor(null);
            setToolTipText(null);
        }
    }

    public HelpTextParagraph(String textString, Desktop desktop, boolean darkMode)
    {
        Color linkColor = darkMode ? LINK_COLOR_DARK : LINK_COLOR_LIGHT;

        StyleConstants.setForeground(linkSet, linkColor);
        StyleConstants.setForeground(linkHoverSet, linkColor);

        Matcher matcher = LINK_PATTERN.matcher(textString);
        int shrinkFactor = 0;
        while (matcher.find())
        {
            MatchResult result = matcher.toMatchResult();
            int start = result.start();
            int end = result.end();

            String text = result.group(1);

            linkRanges.add(new Pair<>(start - shrinkFactor, start + text.length() - shrinkFactor));
            try
            {
                links.add(new URI(result.group(2)));
            } catch (Exception ignored)
            {
                links.add(null);
            }

            shrinkFactor += (end - start) - text.length();
        }

        setText(textString.replaceAll(LINK_PATTERN.pattern(), "$1"));

        for (var range : linkRanges)
        {
            SetStyle(range, linkSet);
        }

        setEditable(false);
        setOpaque(false);
        setFocusable(false);
        setBorder(null);

        addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                int offset = viewToModel2D(e.getPoint());

                boolean hoveringLink = false;
                int i = 0;
                for (var range : linkRanges)
                {
                    if (offset >= range.a() && offset < range.b())
                    {
                        SetStyle(linkRanges.get(i), linkHoverSet);
                        HoverLink(i, true);

                        hoveringLink = true;
                        linkHovered = i;
                        break;
                    }
                    else
                    {
                        SetStyle(linkRanges.get(i), linkSet);
                    }

                    i++;
                }

                if (!hoveringLink)
                {
                    linkHovered = -1;

                    HoverLink(-1, false);
                }
            }
        });

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (linkHovered != -1)
                {
                    URI link = links.get(linkHovered);

                    if (desktop != null && link != null)
                    {
                        try
                        {
                            desktop.browse(link);
                        }
                        catch (Exception ignored) {}
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                for (var range : linkRanges)
                    SetStyle(range, linkSet);

                HoverLink(-1, false);
            }
        });
    }
}
