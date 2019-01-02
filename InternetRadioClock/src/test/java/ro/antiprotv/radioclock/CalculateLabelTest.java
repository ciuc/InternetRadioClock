package ro.antiprotv.radioclock;

import junit.framework.Assert;

import org.junit.Test;


public class CalculateLabelTest  {

    @Test
    public void testLabels() throws Exception{
        System.out.println(calculateLabel("TANANANA"));
        System.out.println(calculateLabel("Radio Guerrilla"));
        System.out.println(calculateLabel("106.4 FM radio block"));
        System.out.println(calculateLabel(""));
    }

    private String calculateLabel(String name) {
        String label ="";
        name = name.toUpperCase();
        name = name.replaceAll("FM", "");
        name = name.replaceAll("RADIO", "");
        name = name.replaceAll("THE", "");
        name = replaceDoubleChars(name, 'b','c','d','f','g','h','h','k','l','m','n','p','r','s','t','v','w','x','z','y');
        name = name.replaceAll("[0-9.!?\\\\-]", "");

        name = name.replaceAll("[AEIOU ]","");


        if (name.length() >= 3) {
            label = name.substring(0, 3);
        }
        return label;
    }

    private String replaceDoubleChars(String name, char... chars) {
        for (char c: chars) {
            String regexp = String.format("%c%c", c,c);

            name = name.replaceAll(regexp.toUpperCase(), String.valueOf(c).toUpperCase());
        }
        return name;
    }
}
