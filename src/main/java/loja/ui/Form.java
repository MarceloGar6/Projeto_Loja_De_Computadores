package loja.ui;

import javax.swing.*;
import java.awt.*;

/** Painel de formulário: uma linha por campo (rótulo à esquerda, campo à direita). */
public class Form extends JPanel {
    private final GridBagConstraints g = new GridBagConstraints();
    private int linha = 0;

    public Form() {
        setLayout(new GridBagLayout());
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
    }

    public void campo(String rotulo, JComponent componente) {
        g.gridy = linha++;
        g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        add(new JLabel(rotulo), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        add(componente, g);
    }
}
