import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestão de Toner - aplicação Java Swing sem dependências externas.
 * Compila com JDK 17+.
 */
public class GestaoTonerApp {
    /**
     * Versão acadêmica/demonstração. Nenhum dado operacional real é distribuído
     * neste projeto público. Integrações externas dependem de configuração local.
     */
    static final boolean ACADEMIC_DEMO_MODE = true;

    static final List<String> DEMO_PRINTERS = List.of(
            "ADMINISTRATIVO - RICOH IM 430 - DEMO-001 - TONER W10",
            "RECEPÇÃO - RICOH IM 430 - DEMO-002 - TONER W10",
            "FINANCEIRO - RICOH MP C307 - DEMO-003 - TONER COLORIDO",
            "RH - SAMSUNG C3060FR - DEMO-004 - TONER COLORIDO",
            "LOGÍSTICA - RICOH IM 430 - DEMO-005 - TONER CF2",
            "ESTOQUE - RICOH IM 430 - DEMO-006 - TONER W12",
            "Outro"
    );

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            Storage storage = new Storage();
            storage.ensureSampleData();
            MainFrame frame = new MainFrame(storage);
            frame.setVisible(true);
        });
    }

    static class MainFrame extends JFrame {
        private final Storage storage;
        private final DefaultListModel<Pedido> listModel = new DefaultListModel<>();
        private final JList<Pedido> pedidoList = new JList<>(listModel);
        private final PromptTextField busca = new PromptTextField("Busque Setor, Nº Série, Check-out, solicitante, nota...");
        private final JLabel resumo = new JLabel();
        private String filtroStatus = "Todos";
        private final JButton btnTodos = new JButton();
        private final JButton btnAguardando = new JButton();
        private final JButton btnCaminho = new JButton();
        private final JButton btnEntregue = new JButton();
        private List<Pedido> pedidos = new ArrayList<>();
        private static final Color BG = new Color(10, 10, 10);
        private static final Color PANEL = new Color(22, 22, 22);
        private static final Color WHITE = new Color(245,245,245);
        private static final Color LIME = new Color(185, 240, 0);
        private static final Color BLUE = new Color(47, 95, 177);
        private static final Color RED = new Color(200, 0, 35);
        private static final Color GREEN = new Color(90, 170, 45);

        MainFrame(Storage storage) {
            this.storage = storage;
            setTitle("Gestão de Toner - MVP Acadêmico");
            setSize(1020, 720);
            setMinimumSize(new Dimension(900, 600));
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());
            getContentPane().setBackground(BG);
            add(header(), BorderLayout.NORTH);
            add(center(), BorderLayout.CENTER);
            add(bottom(), BorderLayout.SOUTH);
            carregar();
        }

        private JComponent header() {
            JPanel root = new JPanel(new BorderLayout(8,8));
            root.setBorder(new EmptyBorder(8,12,10,12));
            root.setBackground(BG);

            JLabel title = new JLabel("Solicitação Toner");
            title.setForeground(WHITE);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
            title.setBorder(new EmptyBorder(0, 0, 0, 0));

            JLabel credit = new JLabel("Created by Elieder");
            credit.setForeground(new Color(145,145,145));
            credit.setFont(credit.getFont().deriveFont(Font.PLAIN, 10f));
            credit.setBorder(new EmptyBorder(1, 4, 0, 0));

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
            titlePanel.add(title);
            titlePanel.add(credit);
            root.add(titlePanel, BorderLayout.WEST);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            JButton solicitar = yellowButton("Solicitar Toner");
            JButton consultaContador = blueButton("Consulta Contador");
            JButton configuracao = darkButton("Configuração ▾");
            JButton atualizar = darkButton("Atualizar");
            Dimension actionSize = new Dimension(112, 34);
            solicitar.setPreferredSize(new Dimension(130, 34));
            consultaContador.setPreferredSize(new Dimension(145, 34));
            configuracao.setPreferredSize(new Dimension(150, 34));
            atualizar.setPreferredSize(actionSize);

            JPopupMenu menuConfiguracao = new JPopupMenu();
            menuConfiguracao.setBorder(new LineBorder(new Color(55,55,55)));
            menuConfiguracao.setBackground(PANEL);
            JMenuItem itemPowerAutomate = menuItem("Configurar Power Automate");
            JMenuItem itemEstoque = menuItem("Estoque");
            JMenuItem itemExportar = menuItem("Exportar CSV");
            JMenuItem itemImprimir = menuItem("Imprimir Lista");
            itemPowerAutomate.addActionListener(e -> configurarEmail());
            itemEstoque.addActionListener(e -> abrirEstoque());
            itemExportar.addActionListener(e -> exportarCsv());
            itemImprimir.addActionListener(e -> imprimirLista());
            menuConfiguracao.add(itemPowerAutomate);
            menuConfiguracao.add(itemEstoque);
            menuConfiguracao.add(itemExportar);
            menuConfiguracao.add(itemImprimir);

            solicitar.addActionListener(e -> solicitarToner());
            consultaContador.addActionListener(e -> abrirConsultaContador());
            configuracao.addActionListener(e -> menuConfiguracao.show(configuracao, 0, configuracao.getHeight()));
            atualizar.addActionListener(e -> carregar());
            actions.add(solicitar);
            actions.add(consultaContador);
            actions.add(configuracao);
            actions.add(atualizar);
            root.add(actions, BorderLayout.EAST);

            JPanel searchLine = new JPanel(new BorderLayout(8,8));
            searchLine.setOpaque(false);
            busca.setFont(busca.getFont().deriveFont(18f));
            busca.setForeground(Color.BLACK);
            busca.setBackground(Color.WHITE);
            busca.setCaretColor(Color.BLACK);
            busca.setBorder(new CompoundBorder(new LineBorder(new Color(70,70,70)), new EmptyBorder(8,12,8,12)));
            busca.setPreferredSize(new Dimension(100, 44));
            busca.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { filtrar(); }});
            searchLine.add(busca, BorderLayout.CENTER);

            JPanel filters = new JPanel(new GridLayout(1,4,8,0));
            filters.setOpaque(false);
            configFilterButton(btnTodos, "Todos", new Color(70,70,70));
            configFilterButton(btnAguardando, Pedido.STATUS_AGUARDANDO, RED);
            configFilterButton(btnCaminho, Pedido.STATUS_CAMINHO, BLUE);
            configFilterButton(btnEntregue, Pedido.STATUS_ENTREGUE, GREEN);
            filters.add(btnTodos); filters.add(btnAguardando); filters.add(btnCaminho); filters.add(btnEntregue);

            JPanel south = new JPanel(new BorderLayout(0,8));
            south.setOpaque(false);
            south.add(searchLine, BorderLayout.NORTH);
            south.add(filters, BorderLayout.SOUTH);
            root.add(south, BorderLayout.SOUTH);
            return root;
        }

        private void configFilterButton(JButton b, String status, Color c) {
            b.setText(status);
            styleButton(b, c, Color.WHITE);
            b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
            b.addActionListener(e -> { filtroStatus = status; filtrar(); });
        }

        private JComponent center() {
            pedidoList.setCellRenderer(new PedidoRenderer());
            pedidoList.setFixedCellHeight(138);
            pedidoList.setBackground(BG);
            pedidoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            pedidoList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) abrirDetalhes(pedidoList.getSelectedValue());
                }
            });
            JScrollPane sp = new JScrollPane(pedidoList);
            sp.setBorder(new EmptyBorder(0,12,0,12));
            sp.getViewport().setBackground(BG);
            return sp;
        }

        private JComponent bottom() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(new EmptyBorder(8,12,8,12));
            p.setBackground(BG);
            resumo.setForeground(new Color(210,210,210));
            p.add(resumo, BorderLayout.WEST);
            return p;
        }

        private void styleButton(JButton b, Color bg, Color fg) {
            b.setUI(new BasicButtonUI());
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBorderPainted(true);
            b.setFocusPainted(false);
            b.setBackground(bg);
            b.setForeground(fg);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setBorder(new CompoundBorder(new LineBorder(bg.darker()), new EmptyBorder(9,12,9,12)));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private void updateFilterButton(JButton b, boolean selected, Color bg) {
            b.setBackground(bg);
            b.setForeground(Color.WHITE);
            b.setBorder(new CompoundBorder(
                    new LineBorder(selected ? LIME : bg.darker(), selected ? 3 : 1),
                    new EmptyBorder(selected ? 7 : 9, 12, selected ? 7 : 9, 12)
            ));
        }

        private JButton yellowButton(String text) {
            JButton b = new JButton(text);
            styleButton(b, new Color(245, 230, 0), Color.BLACK);
            b.setFont(b.getFont().deriveFont(Font.BOLD));
            return b;
        }

        private JButton blueButton(String text) {
            JButton b = new JButton(text);
            styleButton(b, BLUE, Color.WHITE);
            b.setFont(b.getFont().deriveFont(Font.BOLD));
            return b;
        }

        private JButton darkButton(String text) {
            JButton b = new JButton(text);
            styleButton(b, new Color(35,35,35), Color.WHITE);
            return b;
        }

        private JMenuItem menuItem(String text) {
            JMenuItem item = new JMenuItem(text);
            item.setOpaque(true);
            item.setBackground(PANEL);
            item.setForeground(WHITE);
            item.setFont(item.getFont().deriveFont(Font.BOLD, 12f));
            item.setBorder(new EmptyBorder(8, 14, 8, 28));
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { item.setBackground(new Color(45,45,45)); }
                public void mouseExited(MouseEvent e) { item.setBackground(PANEL); }
            });
            return item;
        }

        private void abrirConsultaContador() {
            String[] colunas = {"Impressora", "Contador (demonstração)"};
            Object[][] linhas = {
                    {"ADMINISTRATIVO - DEMO-001", "12450"},
                    {"RECEPÇÃO - DEMO-002", "8320"},
                    {"FINANCEIRO - DEMO-003", "4590"},
                    {"LOGÍSTICA - DEMO-005", "21870"}
            };
            JTable tabela = new JTable(linhas, colunas);
            tabela.setEnabled(false);
            JScrollPane scroll = new JScrollPane(tabela);
            scroll.setPreferredSize(new Dimension(520, 150));
            JOptionPane.showMessageDialog(this, scroll,
                    "Consulta Contador - Dados demonstrativos", JOptionPane.INFORMATION_MESSAGE);
        }

        private Path appDirectory() {
            try {
                Path location = Paths.get(GestaoTonerApp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
                return Files.isRegularFile(location) ? location.getParent() : location;
            } catch (Exception ex) {
                return Paths.get("").toAbsolutePath();
            }
        }

        private boolean isWindows() {
            return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        }

        private void carregar() {
            pedidos = storage.loadPedidos();
            pedidos.sort(Comparator.comparing(Pedido::getDataSolicitacaoTime).reversed());
            filtrar();
        }

        private void atualizarContadores() {
            long aguardando = pedidos.stream().filter(p -> p.status.equals(Pedido.STATUS_AGUARDANDO)).count();
            long caminho = pedidos.stream().filter(p -> p.status.equals(Pedido.STATUS_CAMINHO)).count();
            long entregue = pedidos.stream().filter(p -> p.status.equals(Pedido.STATUS_ENTREGUE)).count();
            btnTodos.setText("Todos  " + pedidos.size());
            btnAguardando.setText("Aguardando  " + aguardando);
            btnCaminho.setText("A Caminho  " + caminho);
            btnEntregue.setText("Entregue  " + entregue);
            updateFilterButton(btnTodos, filtroStatus.equals("Todos"), new Color(70,70,70));
            updateFilterButton(btnAguardando, filtroStatus.equals(Pedido.STATUS_AGUARDANDO), RED);
            updateFilterButton(btnCaminho, filtroStatus.equals(Pedido.STATUS_CAMINHO), BLUE);
            updateFilterButton(btnEntregue, filtroStatus.equals(Pedido.STATUS_ENTREGUE), GREEN);
        }

        private void filtrar() {
            atualizarContadores();
            String q = busca.getText() == null ? "" : busca.getText().trim().toLowerCase(Locale.ROOT);
            listModel.clear();
            for (Pedido p : pedidos) {
                boolean okStatus = filtroStatus.equals("Todos") || p.status.equals(filtroStatus);
                boolean okBusca = q.isBlank() || p.searchText().toLowerCase(Locale.ROOT).contains(q);
                if (okStatus && okBusca) listModel.addElement(p);
            }
            resumo.setText("Exibindo " + listModel.size() + " pedido(s). Dados salvos em: " + storage.dataDir.toAbsolutePath());
        }

        private void novoPedido() {
            PedidoDialog dialog = new PedidoDialog(this, null);
            dialog.setVisible(true);
            Pedido p = dialog.getPedido();
            if (p != null) {
                pedidos.add(p);
                storage.savePedidos(pedidos);
                carregar();
            }
        }

        private void abrirDetalhes(Pedido p) {
            if (p == null) return;
            DetalhesDialog d = new DetalhesDialog(this, p, storage);
            d.setVisible(true);
            if (d.isChanged()) {
                if (d.isDeleted()) pedidos.removeIf(x -> x.id.equals(p.id));
                storage.savePedidos(pedidos);
                carregar();
            }
        }

        private void abrirEstoque() {
            EstoqueDialog d = new EstoqueDialog(this, storage);
            d.setVisible(true);
        }

        private void configurarEmail() {
            EmailConfigDialog d = new EmailConfigDialog(this, storage);
            d.setVisible(true);
        }

        private void solicitarToner() {
            List<String> modelos = new ArrayList<>(DEMO_PRINTERS);
            SolicitarTonerDialog d = new SolicitarTonerDialog(this, storage, modelos);
            d.setVisible(true);
            carregar();
        }

        private void exportarCsv() {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("pedidos-toner.csv"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    storage.exportPedidosCsv(pedidos, chooser.getSelectedFile().toPath());
                    JOptionPane.showMessageDialog(this, "Arquivo exportado com sucesso.");
                } catch (Exception ex) {
                    showError("Erro ao exportar", ex);
                }
            }
        }

        private void imprimirLista() {
            List<Pedido> visiveis = Collections.list(listModel.elements());
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Relatório de Toners");
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                int linhasPorPagina = 28;
                int totalPaginas = (int)Math.ceil(visiveis.size() / (double)linhasPorPagina);
                if (pageIndex >= totalPaginas) return Printable.NO_SUCH_PAGE;
                Graphics2D g = (Graphics2D) graphics;
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                int x = (int) pageFormat.getImageableX();
                int y = (int) pageFormat.getImageableY() + 20;
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.drawString("Relatório de Solicitação de Toners", x, y);
                y += 22;
                g.setFont(new Font("SansSerif", Font.BOLD, 9));
                g.drawString("Chamado", x, y);
                g.drawString("Status", x+90, y);
                g.drawString("Setor / Equipamento", x+205, y);
                g.drawString("Solicitante", x+430, y);
                y += 8; g.drawLine(x, y, x+540, y); y += 14;
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                int start = pageIndex * linhasPorPagina;
                int end = Math.min(start + linhasPorPagina, visiveis.size());
                for (int i=start; i<end; i++) {
                    Pedido p = visiveis.get(i);
                    g.drawString(shorten(p.chamado, 12), x, y);
                    g.drawString(shorten(p.status, 18), x+90, y);
                    g.drawString(shorten(p.departamento + " - " + p.equipamento, 36), x+205, y);
                    g.drawString(shorten(p.solicitante, 18), x+430, y);
                    y += 16;
                }
                return Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) {
                try { job.print(); } catch (Exception ex) { showError("Erro ao imprimir", ex); }
            }
        }
    }

    static class PromptTextField extends JTextField {
        private final String placeholder;

        PromptTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText() != null && !getText().isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(115, 115, 115));
            FontMetrics fm = g2.getFontMetrics();
            Insets insets = getInsets();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, insets.left, y);
            g2.dispose();
        }
    }

    static class PedidoRenderer extends JPanel implements ListCellRenderer<Pedido> {
        private final JLabel chamado = new JLabel();
        private final JLabel status = new JLabel();
        private final JLabel linha1 = new JLabel();
        private final JLabel linha2 = new JLabel();
        private final JLabel dias = new JLabel();
        private final JLabel data = new JLabel();
        private final JLabel arrow = new JLabel("›");

        PedidoRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,new Color(60,60,60)), new EmptyBorder(8,12,8,12)));
            setBackground(new Color(8,8,8));
            JPanel center = new JPanel(new GridLayout(5,1,0,2)); center.setOpaque(false);
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); top.setOpaque(false);
            chamado.setForeground(Color.WHITE); chamado.setFont(chamado.getFont().deriveFont(Font.BOLD, 18f));
            status.setFont(status.getFont().deriveFont(Font.BOLD, 15f));
            top.add(chamado); top.add(status);
            linha1.setForeground(Color.WHITE); linha2.setForeground(new Color(235,235,235));
            dias.setForeground(new Color(190,240,0)); dias.setFont(dias.getFont().deriveFont(Font.BOLD, 15f));
            data.setForeground(Color.WHITE); data.setHorizontalAlignment(SwingConstants.RIGHT); data.setFont(data.getFont().deriveFont(Font.BOLD, 14f));
            JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false); bottom.add(dias, BorderLayout.WEST); bottom.add(data, BorderLayout.EAST);
            center.add(top); center.add(linha1); center.add(linha2); center.add(new JLabel(" ")); center.add(bottom);
            arrow.setFont(new Font("SansSerif", Font.PLAIN, 48)); arrow.setForeground(new Color(190,240,0));
            add(center, BorderLayout.CENTER); add(arrow, BorderLayout.EAST);
        }

        public Component getListCellRendererComponent(JList<? extends Pedido> list, Pedido p, int index, boolean selected, boolean cellHasFocus) {
            setBackground(selected ? new Color(30,30,30) : new Color(8,8,8));
            chamado.setText(p.chamado.isBlank()? p.id : p.chamado);
            status.setText("● " + p.status);
            status.setForeground(statusColor(p.status));
            linha1.setText(p.departamento + (p.equipamento.isBlank()? "" : " - " + p.equipamento) + (p.ip.isBlank()? "" : " - " + p.ip));
            String numeroNota = !safe(p.notaFiscal).isBlank() ? safe(p.notaFiscal) : safe(p.nota);
            String retorno = safe(p.dataCobrancaRetorno).isBlank() ? "" : "     Retorno cobrado: " + formatDateTimeDisplay(p.dataCobrancaRetorno);
            linha2.setText("Nota: " + numeroNota + "     Contador: " + safe(p.contador) + retorno);
            long d = Math.max(0, ChronoUnit.DAYS.between(p.getDataSolicitacaoTime().toLocalDate(), LocalDate.now()));
            dias.setText("Pedido há " + d + (d == 1 ? " dia" : " dias"));
            data.setText(p.getDataSolicitacaoTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
            return this;
        }
    }

    static class DetalhesDialog extends JDialog {
        private final Pedido pedido;
        private final Storage storage;
        private boolean changed = false;
        private boolean deleted = false;
        DetalhesDialog(Frame owner, Pedido pedido, Storage storage) {
            super(owner, "Detalhes do Pedido", true);
            this.pedido = pedido;
            this.storage = storage;
            setSize(600, 690);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());
            getContentPane().setBackground(Color.BLACK);
            add(header(), BorderLayout.NORTH);
            add(content(), BorderLayout.CENTER);
        }
        private JComponent header() {
            JPanel p = new JPanel(new BorderLayout()); p.setBackground(Color.BLACK); p.setBorder(new EmptyBorder(12,12,12,12));
            JLabel title = new JLabel("‹  Detalhes do Pedido"); title.setForeground(Color.WHITE); title.setFont(title.getFont().deriveFont(Font.BOLD, 22f)); p.add(title, BorderLayout.WEST);
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT)); btns.setOpaque(false);
            JButton edit = new JButton("Editar"); JButton cobrar = new JButton("Cobrar Retorno"); JButton entrega = new JButton("Entregue"); JButton del = new JButton("Excluir");
            cobrar.setToolTipText("Enviar e-mail de acompanhamento ao fornecedor");
            styleDialogButton(edit, new Color(60, 60, 60), Color.WHITE);
            styleDialogButton(cobrar, new Color(0, 105, 190), Color.WHITE);
            styleDialogButton(entrega, new Color(90, 170, 45), Color.WHITE);
            styleDialogButton(del, new Color(180, 20, 35), Color.WHITE);
            edit.addActionListener(e -> editar()); cobrar.addActionListener(e -> cobrarFornecedor(cobrar)); entrega.addActionListener(e -> entregar()); del.addActionListener(e -> excluir());
            btns.add(edit); btns.add(cobrar); btns.add(entrega); btns.add(del); p.add(btns, BorderLayout.EAST); return p;
        }
        private JComponent content() {
            JPanel form = new JPanel(new GridBagLayout()); form.setBackground(Color.BLACK); form.setBorder(new EmptyBorder(4,22,22,22));
            GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=0; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; c.anchor=GridBagConstraints.NORTHWEST;
            addLine(form,c,"Nº Chamado", pedido.chamado);
            addLine(form,c,"Departamento", pedido.departamento);
            addLine(form,c,"Equipamento", pedido.equipamento);
            addLine(form,c,"IP", pedido.ip);
            addLine(form,c,"Nº Série", pedido.numeroSerie);
            addLine(form,c,"Complemento", pedido.complemento);
            addLine(form,c,"Contador", pedido.contador);
            addLine(form,c,"Cor / Toner", pedido.corToner);
            addLine(form,c,"Solicitante", pedido.solicitante);
            addLine(form,c,"Data da Solicitação", pedido.getDataSolicitacaoTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            addLine(form,c,"Data da Aprovação", pedido.dataAprovacao);
            addLine(form,c,"Nota Fiscal Eletrônica", pedido.notaFiscal);
            addLine(form,c,"Tipo de Envio", pedido.tipoEnvio);
            addLine(form,c,"Código de Rastreio", pedido.codigoRastreio);
            addLine(form,c,"Data do Recebimento", pedido.dataRecebimento);
            addLine(form,c,"Chave de Acesso NF-e", pedido.chaveNfe);
            addLine(form,c,"Recebido por", pedido.quemRecebeu);
            addLine(form,c,"Status", pedido.status);
            if (!safe(pedido.dataCobrancaRetorno).isBlank()) addLine(form,c,"Retorno cobrado em", formatDateTimeDisplay(pedido.dataCobrancaRetorno));
            JScrollPane sp = new JScrollPane(form); sp.setBorder(null); sp.getViewport().setBackground(Color.BLACK); return sp;
        }
        private void addLine(JPanel form, GridBagConstraints c, String label, String val) {
            JLabel l = new JLabel(label); l.setForeground(new Color(190,240,0)); l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
            form.add(l,c); c.gridy++;
            JTextArea v = new JTextArea(safe(val)); v.setEditable(false); v.setLineWrap(true); v.setWrapStyleWord(true); v.setForeground(Color.WHITE); v.setBackground(Color.BLACK); v.setBorder(new EmptyBorder(0,0,10,0));
            form.add(v,c); c.gridy++;
        }
        private void abrirFiscal() {
            FiscalDialog fd = new FiscalDialog(this, pedido); fd.setVisible(true); if (fd.changed) { changed = true; dispose(); }
        }
        private void editar() {
            PedidoDialog d = new PedidoDialog((Frame)getOwner(), pedido); d.setVisible(true); Pedido novo = d.getPedido(); if (novo != null) { pedido.copyFrom(novo); changed = true; dispose(); }
        }
        private void cobrarFornecedor(JButton botao) {
            EmailConfig cfg = storage.loadEmailConfig();
            if (!cfg.isComplete()) {
                JOptionPane.showMessageDialog(this,
                        "Configure a URL do Power Automate e o e-mail do fornecedor antes de enviar o acompanhamento.");
                EmailConfigDialog d = new EmailConfigDialog((Frame) getOwner(), storage);
                d.setVisible(true);
                return;
            }

            String chamadoTexto = safe(pedido.chamado).isBlank() ? pedido.id : pedido.chamado;
            String impressora = descricaoImpressoraPedido();
            String contadorTexto = safe(pedido.contador);
            String solicitanteTexto = safe(pedido.solicitante);
            String assunto = "Analisar solicitação de Toner";
            String observacoes = "Por gentileza analisar a solicitação abaixo";
            String tonerFluxo = "AGUARDANDO RETORNO ENVIO - " + impressora;

            int op = JOptionPane.showConfirmDialog(this,
                    "Enviar e-mail de acompanhamento ao fornecedor sobre o chamado " + chamadoTexto + "?",
                    "Aguardar retorno do fornecedor", JOptionPane.YES_NO_OPTION);
            if (op != JOptionPane.YES_OPTION) return;

            String plainBody = "Olá,\n\n"
                    + "Gostaria de verificar o status de envio do toner solicitado.\n\n"
                    + "Número do chamado: " + chamadoTexto + "\n"
                    + "Impressora: " + impressora + "\n"
                    + (contadorTexto.isBlank() ? "" : "Contador: " + contadorTexto + "\n")
                    + (solicitanteTexto.isBlank() ? "" : "Solicitante: " + solicitanteTexto + "\n")
                    + "Status atual: " + safe(pedido.status) + "\n\n"
                    + "Por gentileza analisar a solicitação abaixo\n\n"
                    + "Atenciosamente.";
            String htmlBody = buildCobrancaHtmlEmail(chamadoTexto, impressora, contadorTexto, solicitanteTexto, safe(pedido.status));

            botao.setEnabled(false);
            botao.setText("Enviando...");
            new SwingWorker<EmailDispatchResult, Void>() {
                @Override protected EmailDispatchResult doInBackground() throws Exception {
                    PowerAutomateSender.send(cfg, assunto, plainBody, htmlBody,
                            chamadoTexto, tonerFluxo, contadorTexto, solicitanteTexto, observacoes, null);
                    return new EmailDispatchResult(true, false,
                            ACADEMIC_DEMO_MODE
                                    ? "Modo demonstração: o acompanhamento foi simulado e nenhum dado foi enviado para fora do computador."
                                    : "Acompanhamento enviado com sucesso para o Power Automate.\n\nO fluxo configurado enviará o e-mail ao fornecedor: " + cfg.fornecedorEmail + ".");
                }

                @Override protected void done() {
                    botao.setEnabled(true);
                    botao.setText("Cobrar Retorno");
                    try {
                        EmailDispatchResult result = get();
                        pedido.dataCobrancaRetorno = LocalDateTime.now().toString();
                        changed = true;
                        JOptionPane.showMessageDialog(DetalhesDialog.this, result.message);
                        dispose();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(DetalhesDialog.this,
                                "Não foi possível enviar o acompanhamento.\n\n" + cause.getMessage(),
                                "Erro ao enviar", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private String descricaoImpressoraPedido() {
            String base = !safe(pedido.corToner).isBlank() ? safe(pedido.corToner) : safe(pedido.departamento);
            if (base.isBlank()) base = safe(pedido.equipamento);
            if (!safe(pedido.equipamento).isBlank() && !base.contains(pedido.equipamento)) base += " - " + pedido.equipamento;
            if (!safe(pedido.ip).isBlank() && !base.contains(pedido.ip)) base += " - " + pedido.ip;
            return base.trim();
        }

        private void entregar() {
            JPanel p = new JPanel(new GridLayout(6,1,4,4));
            String[] nomesRecebidoPor = {"", "USUÁRIO 1", "USUÁRIO 2", "USUÁRIO 3"};
            JComboBox<String> quem = new JComboBox<>(nomesRecebidoPor);
            quem.setEditable(false);
            if (pedido.quemRecebeu != null && !pedido.quemRecebeu.isBlank()) {
                boolean encontrado = false;
                for (String nome : nomesRecebidoPor) {
                    if (nome.equalsIgnoreCase(pedido.quemRecebeu)) {
                        quem.setSelectedItem(nome);
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) {
                    quem.addItem(pedido.quemRecebeu);
                    quem.setSelectedItem(pedido.quemRecebeu);
                }
            } else {
                quem.setSelectedIndex(0);
            }
            JTextField chaveNfe = new JTextField(pedido.chaveNfe);
            JTextField data = new JTextField(
                    pedido.dataRecebimento == null || pedido.dataRecebimento.isBlank()
                            ? LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : pedido.dataRecebimento
            );
            p.add(new JLabel("Recebido por"));
            p.add(quem);
            p.add(new JLabel("Chave de Acesso NF-e"));
            p.add(chaveNfe);
            p.add(new JLabel("Data do recebimento"));
            p.add(data);
            Object[] opcoes = {"Entregue", "Cancelar"};
            if (JOptionPane.showOptionDialog(this, p, "Marcar como Entregue", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opcoes, opcoes[0]) == 0) {
                String selecionado = String.valueOf(quem.getSelectedItem()).trim();
                if (selecionado.isBlank()) {
                    JOptionPane.showMessageDialog(this, "Selecione o nome de quem recebeu.", "Campo obrigatório", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                pedido.status = Pedido.STATUS_ENTREGUE;
                pedido.quemRecebeu = selecionado;
                pedido.chaveNfe = chaveNfe.getText();
                pedido.dataRecebimento = data.getText();
                changed = true;
                dispose();
            }
        }
        private void excluir() {
            int op = JOptionPane.showConfirmDialog(this,"Deseja excluir este pedido?","Confirmar",JOptionPane.YES_NO_OPTION); if (op==JOptionPane.YES_OPTION) { deleted=true; changed=true; dispose(); }
        }
        boolean isChanged() { return changed; }
        boolean isDeleted() { return deleted; }
    }

    static class FiscalDialog extends JDialog {
        boolean changed=false;
        private final Pedido pedido;
        FiscalDialog(Dialog owner, Pedido p) {
            super(owner,"Dados Fiscais",true); this.pedido = p; setSize(470,520); setLocationRelativeTo(owner); build();
        }
        private void build() {
            JPanel root = new JPanel(new BorderLayout()); root.setBackground(Color.BLACK);
            JLabel title = new JLabel("  Dados Fiscais"); title.setOpaque(true); title.setBackground(Color.BLACK); title.setForeground(Color.WHITE); title.setFont(title.getFont().deriveFont(Font.BOLD,22f)); title.setBorder(new EmptyBorder(12,8,12,8));
            root.add(title,BorderLayout.NORTH);
            JPanel form = new JPanel(new GridBagLayout()); form.setBackground(new Color(10,140,185)); form.setBorder(new EmptyBorder(12,22,12,22)); GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=0; c.fill=GridBagConstraints.HORIZONTAL; c.weightx=1;
            JLabel aprovado = new JLabel("Pedido Aprovado", SwingConstants.CENTER); aprovado.setForeground(Color.WHITE); aprovado.setFont(aprovado.getFont().deriveFont(Font.BOLD,18f)); form.add(aprovado,c); c.gridy++;
            JTextField dataAprov = field(pedido.dataAprovacao.isBlank()?LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")):pedido.dataAprovacao);
            JTextField nf = field(pedido.notaFiscal);
            JComboBox<String> tipo = new JComboBox<>(new String[]{"", "Técnico", "Correios", "Outros"}); tipo.setSelectedItem(pedido.tipoEnvio);
            JTextField receb = field(pedido.dataRecebimento);
            JTextField chave = field(pedido.chaveNfe);
            JComboBox<String> status = new JComboBox<>(new String[]{Pedido.STATUS_AGUARDANDO, Pedido.STATUS_CAMINHO, Pedido.STATUS_ENTREGUE}); status.setSelectedItem(pedido.status);
            addField(form,c,"Data da Aprovação",dataAprov); addField(form,c,"Nota Fiscal Eletrônica",nf); addField(form,c,"Tipo de Envio",tipo); addField(form,c,"Data do Recebimento",receb); addField(form,c,"Chave de Acesso NF-e",chave); addField(form,c,"Status",status);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.setBackground(Color.BLACK); JButton cancel = new JButton("Cancelar"); JButton save = new JButton("✓ Salvar"); save.setBackground(new Color(245,230,0)); save.setForeground(Color.BLACK); cancel.addActionListener(e->dispose()); save.addActionListener(e->{ pedido.dataAprovacao=dataAprov.getText(); pedido.notaFiscal=nf.getText(); pedido.tipoEnvio=Objects.toString(tipo.getSelectedItem(),""); pedido.dataRecebimento=receb.getText(); pedido.chaveNfe=chave.getText(); pedido.status=Objects.toString(status.getSelectedItem(),Pedido.STATUS_AGUARDANDO); if (!pedido.tipoEnvio.isBlank() && !Pedido.STATUS_ENTREGUE.equals(pedido.status)) pedido.status=Pedido.STATUS_CAMINHO; changed=true; dispose(); }); buttons.add(cancel); buttons.add(save);
            root.add(form,BorderLayout.CENTER); root.add(buttons,BorderLayout.SOUTH); setContentPane(root);
        }
        private JTextField field(String s){ JTextField f=new JTextField(safe(s)); f.setFont(f.getFont().deriveFont(16f)); return f; }
        private void addField(JPanel form, GridBagConstraints c, String label, JComponent input) { JLabel l=new JLabel(label); l.setForeground(Color.WHITE); l.setFont(l.getFont().deriveFont(15f)); l.setBorder(new EmptyBorder(10,0,4,0)); form.add(l,c); c.gridy++; form.add(input,c); c.gridy++; }
    }

    static class PedidoDialog extends JDialog {
        private Pedido result;
        private final Pedido original;
        private final Map<String,JTextField> fields = new LinkedHashMap<>();
        private JComboBox<String> status;
        private JComboBox<String> departamentoCombo;
        private JComboBox<String> modeloTonerCombo;
        private JComboBox<String> tipoEnvioCombo;
        private JLabel rastreioLabel;
        private JTextField rastreioField;
        PedidoDialog(Frame owner, Pedido original) {
            super(owner, original==null?"Novo Pedido":"Editar Pedido", true);
            this.original = original;
            setSize(700,620); setLocationRelativeTo(owner); build();
        }
        private void build() {
            JPanel root = new JPanel(new BorderLayout()); root.setBorder(new EmptyBorder(12,12,12,12));
            JPanel form = new JPanel(new GridBagLayout()); GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=0; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; c.insets=new Insets(4,4,4,4);
            Pedido p = original==null? new Pedido() : original;
            add(form,c,"Nº Chamado","chamado", p.chamado);
            addModeloToner(form,c,!safe(p.corToner).isBlank() ? p.corToner : p.departamento);
            add(form,c,"Contador","contador", p.contador);
            add(form,c,"Solicitante","solicitante", p.solicitante);
            add(form,c,"Data da Solicitação (dd/MM/yyyy HH:mm)","dataSolicitacao", p.dataSolicitacao.isBlank()? LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : p.getDataSolicitacaoTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            add(form,c,"Data da Aprovação","dataAprovacao", p.dataAprovacao);
            add(form,c,"Nota Fiscal Eletrônica","notaFiscal", p.notaFiscal);
            addTipoEnvio(form,c,p.tipoEnvio);
            addRastreio(form,c,p.codigoRastreio);
            add(form,c,"Data do Recebimento","dataRecebimento", p.dataRecebimento);
            add(form,c,"Chave de Acesso NF-e","chaveNfe", p.chaveNfe);
            add(form,c,"Recebido por","quemRecebeu", p.quemRecebeu);
            JLabel lab = new JLabel("Status"); form.add(lab,c); c.gridy++; status = new JComboBox<>(new String[]{Pedido.STATUS_AGUARDANDO, Pedido.STATUS_CAMINHO, Pedido.STATUS_ENTREGUE}); status.setSelectedItem(p.status.isBlank()?Pedido.STATUS_AGUARDANDO:p.status); form.add(status,c); c.gridy++;
            JScrollPane scroll = new JScrollPane(form); root.add(scroll, BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); JButton cancel = new JButton("Cancelar"); JButton save = new JButton("Salvar"); save.setBackground(new Color(245,230,0)); cancel.addActionListener(e->dispose()); save.addActionListener(e->salvar()); buttons.add(cancel); buttons.add(save); root.add(buttons,BorderLayout.SOUTH); setContentPane(root);
        }
        private String[] printerOptions() {
            return DEMO_PRINTERS.toArray(new String[0]);
        }

        private void addDepartamento(JPanel form, GridBagConstraints c, String value) {
            JLabel l = new JLabel("Departamento / Setor");
            form.add(l,c); c.gridy++;
            String[] options = printerOptions();
            departamentoCombo = new JComboBox<>(options);
            departamentoCombo.setFont(departamentoCombo.getFont().deriveFont(15f));
            departamentoCombo.setEditable(false);
            if (value != null && !value.isBlank()) {
                boolean found = false;
                for (String option : options) {
                    if (option.equals(value)) { found = true; break; }
                }
                if (!found) departamentoCombo.addItem(value);
                departamentoCombo.setSelectedItem(value);
            } else {
                departamentoCombo.setSelectedIndex(0);
            }
            form.add(departamentoCombo,c); c.gridy++;
        }

        private void addModeloToner(JPanel form, GridBagConstraints c, String value) {
            JLabel l = new JLabel("Modelo / descrição do toner");
            form.add(l,c); c.gridy++;
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (String option : printerOptions()) {
                model.addElement(option);
            }
            modeloTonerCombo = new JComboBox<>(model);
            modeloTonerCombo.setFont(modeloTonerCombo.getFont().deriveFont(15f));
            modeloTonerCombo.setEditable(true);
            Component editor = modeloTonerCombo.getEditor().getEditorComponent();
            if (editor instanceof JTextField tf) tf.setFont(tf.getFont().deriveFont(15f));
            if (value != null && !value.isBlank()) {
                boolean found = false;
                for (int i = 0; i < modeloTonerCombo.getItemCount(); i++) {
                    if (value.equals(modeloTonerCombo.getItemAt(i))) { found = true; break; }
                }
                if (!found) modeloTonerCombo.addItem(value);
                modeloTonerCombo.setSelectedItem(value);
            } else {
                modeloTonerCombo.setSelectedItem("");
            }
            form.add(modeloTonerCombo,c); c.gridy++;
        }

        private void addTipoEnvio(JPanel form, GridBagConstraints c, String value) {
            JLabel l = new JLabel("Tipo de Envio");
            form.add(l,c); c.gridy++;
            String valor = safe(value);
            if ("Correios".equalsIgnoreCase(valor)) valor = "Correio";
            tipoEnvioCombo = new JComboBox<>(new String[]{"", "Correio", "Técnico"});
            tipoEnvioCombo.setFont(tipoEnvioCombo.getFont().deriveFont(15f));
            tipoEnvioCombo.setSelectedItem(valor);
            tipoEnvioCombo.addActionListener(e -> { atualizarCampoRastreio(); atualizarStatusPorTipoEnvio(); });
            form.add(tipoEnvioCombo,c); c.gridy++;
        }

        private void addRastreio(JPanel form, GridBagConstraints c, String value) {
            rastreioLabel = new JLabel("Código de Rastreio");
            form.add(rastreioLabel,c); c.gridy++;
            rastreioField = new JTextField(safe(value));
            rastreioField.setFont(rastreioField.getFont().deriveFont(15f));
            form.add(rastreioField,c); c.gridy++;
            atualizarCampoRastreio();
        }

        private void atualizarCampoRastreio() {
            boolean mostrar = "Correio".equals(Objects.toString(tipoEnvioCombo.getSelectedItem(), ""));
            if (rastreioLabel != null) rastreioLabel.setVisible(mostrar);
            if (rastreioField != null) rastreioField.setVisible(mostrar);
            if (!mostrar && rastreioField != null) rastreioField.setText("");
            if (rastreioField != null) {
                Container parent = rastreioField.getParent();
                if (parent != null) {
                    parent.revalidate();
                    parent.repaint();
                }
            }
        }

        private void atualizarStatusPorTipoEnvio() {
            if (tipoEnvioCombo == null || status == null) return;
            String tipo = Objects.toString(tipoEnvioCombo.getSelectedItem(), "").trim();
            String atual = Objects.toString(status.getSelectedItem(), "");
            if (!tipo.isBlank() && !Pedido.STATUS_ENTREGUE.equals(atual)) {
                status.setSelectedItem(Pedido.STATUS_CAMINHO);
            }
        }

        private void add(JPanel form, GridBagConstraints c, String label, String key, String value) { JLabel l = new JLabel(label); form.add(l,c); c.gridy++; JTextField f = new JTextField(safe(value)); f.setFont(f.getFont().deriveFont(15f)); fields.put(key,f); form.add(f,c); c.gridy++; }
        private void salvar() {
            Pedido p = new Pedido();
            if (original != null) {
                p.id = original.id;
                p.equipamento = original.equipamento;
                p.numeroSerie = original.numeroSerie;
                p.complemento = original.complemento;
                p.corToner = original.corToner;
                p.nota = original.nota;
                p.codigoRastreio = original.codigoRastreio;
            }
            p.chamado = v("chamado"); p.corToner=safe(Objects.toString(modeloTonerCombo.getEditor().getItem(), "")).trim(); p.departamento=p.corToner; p.ip=original != null ? original.ip : ""; p.contador=v("contador"); p.solicitante=v("solicitante"); p.dataSolicitacao=parseToIso(v("dataSolicitacao")); p.dataAprovacao=v("dataAprovacao"); p.notaFiscal=v("notaFiscal"); p.tipoEnvio=Objects.toString(tipoEnvioCombo.getSelectedItem(), ""); p.codigoRastreio="Correio".equals(p.tipoEnvio) ? safe(rastreioField.getText()).trim() : ""; p.dataRecebimento=v("dataRecebimento"); p.chaveNfe=v("chaveNfe"); p.quemRecebeu=v("quemRecebeu"); p.status=Objects.toString(status.getSelectedItem(),Pedido.STATUS_AGUARDANDO);
            if (p.chamado.isBlank()) p.chamado = p.id;
            result = p; dispose();
        }
        private String v(String k){ return fields.get(k).getText().trim(); }
        Pedido getPedido(){ return result; }
    }

    static class EmailConfigDialog extends JDialog {
        private final Storage storage;
        private final JTextField powerAutomateUrl = new JTextField();
        private final JTextField fornecedor = new JTextField();
        private final JTextField copia = new JTextField();

        EmailConfigDialog(Frame owner, Storage storage) {
            super(owner, "Configurar Power Automate", true);
            this.storage = storage;
            setSize(620, 440);
            setLocationRelativeTo(owner);
            build();
            carregar();
        }

        private void build() {
            JPanel root = new JPanel(new BorderLayout(10,10));
            root.setBorder(new EmptyBorder(12,12,12,12));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(4,4,4,4);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;

            JLabel modo = new JLabel("Modo de envio: Power Automate");
            modo.setFont(modo.getFont().deriveFont(Font.BOLD, 16f));
            form.add(modo, c); c.gridy++;

            addField(form, c, "URL do fluxo Power Automate", powerAutomateUrl);
            addField(form, c, "E-mail do fornecedor", fornecedor);
            addField(form, c, "E-mails em cópia (CC)", copia);

            JTextArea info = new JTextArea(
                    "Como configurar:\n" +
                    "1. No Power Automate, crie um fluxo com o gatilho 'Quando uma solicitação HTTP for recebida'.\n" +
                    "2. Copie a URL gerada pelo gatilho e cole no campo acima.\n" +
                    "3. No fluxo, use os campos recebidos para enviar e-mail, criar aprovação, registrar em planilha, SharePoint ou Teams.\n\n" +
                    "O sistema enviará um JSON com chamado, toner, contador, solicitante, observações, assunto, fornecedor, CC, corpo em texto e corpo em HTML.");
            info.setEditable(false);
            info.setOpaque(false);
            info.setLineWrap(true);
            info.setWrapStyleWord(true);
            info.setForeground(new Color(80,80,80));
            form.add(info, c); c.gridy++;

            root.add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelar = new JButton("Cancelar");
            JButton salvar = new JButton("Salvar");
            salvar.setBackground(new Color(245,230,0));
            salvar.setForeground(Color.BLACK);
            cancelar.addActionListener(e -> dispose());
            salvar.addActionListener(e -> salvar());
            buttons.add(cancelar);
            buttons.add(salvar);
            root.add(buttons, BorderLayout.SOUTH);
            setContentPane(root);
        }

        private void addField(JPanel form, GridBagConstraints c, String label, JTextField field) {
            form.add(new JLabel(label), c); c.gridy++;
            field.setFont(field.getFont().deriveFont(15f));
            form.add(field, c); c.gridy++;
        }

        private void carregar() {
            EmailConfig cfg = storage.loadEmailConfig();
            powerAutomateUrl.setText(cfg.powerAutomateUrl);
            fornecedor.setText(cfg.fornecedorEmail);
            copia.setText(cfg.copiaEmail);
        }

        private void salvar() {
            EmailConfig cfg = storage.loadEmailConfig();
            cfg.modoEnvio = EmailConfig.MODO_POWER_AUTOMATE;
            cfg.powerAutomateUrl = powerAutomateUrl.getText().trim();
            cfg.fornecedorEmail = fornecedor.getText().trim();
            cfg.copiaEmail = copia.getText().trim();

            if (cfg.powerAutomateUrl.isBlank()) {
                JOptionPane.showMessageDialog(this, "Informe a URL do fluxo Power Automate.");
                return;
            }
            if (!cfg.powerAutomateUrl.startsWith("http://") && !cfg.powerAutomateUrl.startsWith("https://")) {
                JOptionPane.showMessageDialog(this, "A URL do Power Automate deve começar com http:// ou https://.");
                return;
            }
            if (cfg.fornecedorEmail.isBlank()) {
                JOptionPane.showMessageDialog(this, "Informe o e-mail do fornecedor.");
                return;
            }
            storage.saveEmailConfig(cfg);
            JOptionPane.showMessageDialog(this, "Configuração do Power Automate salva com sucesso.");
            dispose();
        }
    }

    static class EmailConfig {
        static final String MODO_POWER_AUTOMATE = "Power Automate";

        String modoEnvio = MODO_POWER_AUTOMATE;
        String powerAutomateUrl = "";
        String remetente = "";
        String senha = "";
        String smtpHost = "smtp.example.com";
        int porta = 587;
        String seguranca = "STARTTLS";
        String fornecedorEmail = "";
        String copiaEmail = "";

        boolean isComplete() {
            return !powerAutomateUrl.isBlank() && !fornecedorEmail.isBlank();
        }
    }

    static class EmailDispatchResult {
        final boolean sent;
        final boolean opened;
        final String message;
        EmailDispatchResult(boolean sent, boolean opened, String message) {
            this.sent = sent;
            this.opened = opened;
            this.message = message;
        }
    }

    static class AttachmentData {
        static final long MAX_BYTES = 10L * 1024L * 1024L;
        final String nome;
        final String tipo;
        final long tamanho;
        final String conteudoBase64;

        AttachmentData(String nome, String tipo, long tamanho, String conteudoBase64) {
            this.nome = nome;
            this.tipo = tipo;
            this.tamanho = tamanho;
            this.conteudoBase64 = conteudoBase64;
        }

        static AttachmentData fromFile(File file) throws IOException {
            if (file == null) return null;
            Path path = file.toPath();
            if (!Files.exists(path)) throw new FileNotFoundException("Arquivo anexado não encontrado: " + file.getAbsolutePath());
            long size = Files.size(path);
            if (size > MAX_BYTES) {
                throw new IOException("O arquivo selecionado é maior que 10 MB. Selecione um arquivo menor.");
            }
            String type = Files.probeContentType(path);
            if (type == null || type.isBlank()) type = "application/octet-stream";
            String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(path));
            return new AttachmentData(file.getName(), type, size, b64);
        }
    }

    static class PowerAutomateSender {
        static void send(EmailConfig cfg, String subject, String plainBody, String htmlBody,
                         String chamado, String toner, String contador, String solicitante, String observacoes, AttachmentData anexo) throws Exception {
            // Segurança: a versão acadêmica nunca realiza chamadas externas.
            if (ACADEMIC_DEMO_MODE) return;
            if (cfg.powerAutomateUrl == null || cfg.powerAutomateUrl.isBlank()) {
                throw new Exception("URL do Power Automate não configurada.");
            }
            URI uri;
            try {
                uri = URI.create(cfg.powerAutomateUrl.trim());
            } catch (Exception ex) {
                throw new Exception("URL do Power Automate inválida.");
            }

            String payload = buildPayload(cfg, subject, plainBody, htmlBody, chamado, toner, contador, solicitante, observacoes, anexo);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String response = readResponse(conn, status >= 200 && status < 300);
            conn.disconnect();

            if (status < 200 || status >= 300) {
                String detail = response.isBlank() ? "Sem detalhes retornados pelo Power Automate." : response;
                throw new Exception("Power Automate retornou HTTP " + status + ".\n" + detail);
            }
        }

        private static String buildPayload(EmailConfig cfg, String subject, String plainBody, String htmlBody,
                                           String chamado, String toner, String contador, String solicitante, String observacoes, AttachmentData anexo) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            appendJsonField(sb, "sistema", "Gestão de Toner");
            appendJsonField(sb, "tipo", "solicitacao_toner");
            appendJsonField(sb, "dataEnvio", LocalDateTime.now().toString());
            appendJsonField(sb, "modoEnvio", EmailConfig.MODO_POWER_AUTOMATE);
            appendJsonField(sb, "numeroChamado", chamado);
            appendJsonField(sb, "toner", toner);
            appendJsonField(sb, "contador", contador);
            appendJsonField(sb, "solicitante", solicitante);
            appendJsonField(sb, "observacoes", observacoes);
            appendJsonField(sb, "status", Pedido.STATUS_AGUARDANDO);
            appendJsonField(sb, "assunto", subject);
            appendJsonField(sb, "fornecedorEmail", cfg.fornecedorEmail);
            appendJsonField(sb, "copiaEmail", cfg.copiaEmail);
            appendJsonArray(sb, "copiaEmails", splitEmails(cfg.copiaEmail));
            appendJsonField(sb, "temAnexo", anexo == null ? "false" : "true");
            appendJsonField(sb, "anexoNome", anexo == null ? "" : anexo.nome);
            appendJsonField(sb, "anexoTipo", anexo == null ? "" : anexo.tipo);
            appendJsonField(sb, "anexoTamanho", anexo == null ? "" : Long.toString(anexo.tamanho));
            appendJsonField(sb, "anexoBase64", anexo == null ? "" : anexo.conteudoBase64);
            appendJsonAttachmentArray(sb, "anexos", anexo);
            appendJsonEmailAttachmentArray(sb, "emailAttachments", anexo);
            appendJsonField(sb, "corpoTexto", plainBody);
            appendJsonField(sb, "corpoHtml", htmlBody);
            if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
            sb.append("}");
            return sb.toString();
        }

        private static void appendJsonField(StringBuilder sb, String key, String value) {
            sb.append('"').append(jsonEscape(key)).append("\":");
            sb.append('"').append(jsonEscape(value)).append('"').append(',');
        }

        private static void appendJsonArray(StringBuilder sb, String key, List<String> values) {
            sb.append('"').append(jsonEscape(key)).append("\":");
            sb.append('[');
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(jsonEscape(values.get(i))).append('"');
            }
            sb.append(']').append(',');
        }

        private static void appendJsonAttachmentArray(StringBuilder sb, String key, AttachmentData anexo) {
            sb.append('"').append(jsonEscape(key)).append("\":");
            sb.append('[');
            if (anexo != null) {
                sb.append('{');
                appendJsonField(sb, "nome", anexo.nome);
                appendJsonField(sb, "tipo", anexo.tipo);
                appendJsonField(sb, "tamanho", Long.toString(anexo.tamanho));
                appendJsonField(sb, "conteudoBase64", anexo.conteudoBase64);
                if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
                sb.append('}');
            }
            sb.append(']').append(',');
        }

        private static void appendJsonEmailAttachmentArray(StringBuilder sb, String key, AttachmentData anexo) {
            sb.append('"').append(jsonEscape(key)).append("\":");
            sb.append('[');
            if (anexo != null) {
                sb.append('{');
                appendJsonField(sb, "Name", anexo.nome);
                appendJsonBinaryContentObject(sb, "ContentBytes", anexo.tipo, anexo.conteudoBase64);
                if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
                sb.append('}');
            }
            sb.append(']').append(',');
        }

        private static void appendJsonBinaryContentObject(StringBuilder sb, String key, String contentType, String base64Content) {
            sb.append('"').append(jsonEscape(key)).append("\":");
            sb.append('{');
            appendJsonField(sb, "$content-type", contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);
            appendJsonField(sb, "$content", base64Content);
            if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
            sb.append('}').append(',');
        }

        private static String jsonEscape(String value) {
            String s = safe(value);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                switch (ch) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\b' -> out.append("\\b");
                    case '\f' -> out.append("\\f");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (ch < 32) out.append(String.format("\\u%04x", (int) ch));
                        else out.append(ch);
                    }
                }
            }
            return out.toString();
        }

        private static String readResponse(HttpURLConnection conn, boolean ok) {
            InputStream stream = null;
            try {
                stream = ok ? conn.getInputStream() : conn.getErrorStream();
                if (stream == null) return "";
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = stream.read(buffer)) >= 0) baos.write(buffer, 0, n);
                return baos.toString(StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return "";
            } finally {
                try { if (stream != null) stream.close(); } catch (Exception ignored) {}
            }
        }
    }

    static class OutlookSender {
        static void send(EmailConfig cfg, String subject, String plainBody, String htmlBody) throws Exception {
            if (cfg.fornecedorEmail == null || cfg.fornecedorEmail.isBlank()) {
                throw new Exception("E-mail do fornecedor não configurado.");
            }

            String subjectB64 = Base64.getEncoder().encodeToString(safe(subject).getBytes(StandardCharsets.UTF_8));
            String htmlB64 = Base64.getEncoder().encodeToString(safe(htmlBody).getBytes(StandardCharsets.UTF_8));

            String script = "$ErrorActionPreference = 'Stop'\r\n"
                    + "$outlook = New-Object -ComObject Outlook.Application\r\n"
                    + "$mail = $outlook.CreateItem(0)\r\n"
                    + "$remetente = " + psQuote(cfg.remetente) + "\r\n"
                    + "if (-not [string]::IsNullOrWhiteSpace($remetente)) {\r\n"
                    + "  $accountFound = $false\r\n"
                    + "  foreach ($account in $outlook.Session.Accounts) {\r\n"
                    + "    if ($account.SmtpAddress -ieq $remetente) {\r\n"
                    + "      $mail.SendUsingAccount = $account\r\n"
                    + "      $accountFound = $true\r\n"
                    + "      break\r\n"
                    + "    }\r\n"
                    + "  }\r\n"
                    + "  if (-not $accountFound) { throw 'Conta remetente nao encontrada no Outlook: ' + $remetente }\r\n"
                    + "}\r\n"
                    + "$mail.To = " + psQuote(cfg.fornecedorEmail) + "\r\n"
                    + "$mail.CC = " + psQuote(cfg.copiaEmail) + "\r\n"
                    + "$mail.Subject = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(" + psQuote(subjectB64) + "))\r\n"
                    + "$mail.HTMLBody = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(" + psQuote(htmlB64) + "))\r\n"
                    + "$mail.Send()\r\n";

            Path ps1 = Files.createTempFile("gestao-toner-outlook-", ".ps1");
            try {
                Files.write(ps1, script.getBytes(StandardCharsets.UTF_8));
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-ExecutionPolicy", "Bypass",
                        "-WindowStyle", "Hidden",
                        "-File", ps1.toAbsolutePath().toString()
                );
                pb.redirectErrorStream(true);
                Process p = pb.start();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (InputStream in = p.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) >= 0) baos.write(buffer, 0, n);
                }
                int exit = p.waitFor();
                String output = baos.toString(StandardCharsets.UTF_8);
                if (exit != 0) {
                    if (output.isBlank()) output = "Falha ao acionar o Outlook pelo PowerShell.";
                    throw new Exception(output.trim());
                }
            } finally {
                try { Files.deleteIfExists(ps1); } catch (Exception ignored) {}
            }
        }

        private static String psQuote(String value) {
            return "'" + safe(value).replace("'", "''") + "'";
        }
    }

    static class DefaultMailClient {
        static void open(EmailConfig cfg, String subject, String body) throws Exception {
            if (cfg.fornecedorEmail == null || cfg.fornecedorEmail.isBlank()) {
                throw new Exception("E-mail do fornecedor não configurado.");
            }
            String mailto = "mailto:" + encodeMail(cfg.fornecedorEmail)
                    + "?subject=" + encodeMail(subject)
                    + (safe(cfg.copiaEmail).isBlank() ? "" : "&cc=" + encodeMail(joinEmails(cfg.copiaEmail)))
                    + "&body=" + encodeMail(body);
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                throw new UnsupportedOperationException("Não há cliente de e-mail padrão configurado no Windows.");
            }
            Desktop.getDesktop().mail(new URI(mailto));
        }
    }

    static class SimpleSmtpSender {
        static void send(EmailConfig cfg, String subject, String plainBody, String htmlBody) throws Exception {
            SmtpConnection conn = new SmtpConnection(cfg);
            try {
                conn.connect();
                conn.send("EHLO localhost", 250);
                if ("STARTTLS".equalsIgnoreCase(cfg.seguranca)) {
                    conn.send("STARTTLS", 220);
                    conn.startTls();
                    conn.send("EHLO localhost", 250);
                }
                conn.send("AUTH LOGIN", 334);
                conn.send(Base64.getEncoder().encodeToString(cfg.remetente.getBytes(StandardCharsets.UTF_8)), 334);
                conn.send(Base64.getEncoder().encodeToString(cfg.senha.getBytes(StandardCharsets.UTF_8)), 235);
                conn.send("MAIL FROM:<" + cfg.remetente + ">", 250);
                conn.send("RCPT TO:<" + cfg.fornecedorEmail + ">", 250, 251);
                for (String cc : splitEmails(cfg.copiaEmail)) {
                    conn.send("RCPT TO:<" + cc + ">", 250, 251);
                }
                conn.send("DATA", 354);
                conn.writeData(buildMessage(cfg, subject, plainBody, htmlBody));
                conn.expect(250);
                conn.send("QUIT", 221);
            } finally {
                conn.close();
            }
        }

        private static String buildMessage(EmailConfig cfg, String subject, String plainBody, String htmlBody) {
            String boundary = "----=_Part_" + UUID.randomUUID();
            StringBuilder sb = new StringBuilder();
            sb.append("From: <").append(cfg.remetente).append(">\r\n");
            sb.append("To: <").append(cfg.fornecedorEmail).append(">\r\n");
            String ccHeader = joinEmails(cfg.copiaEmail);
            if (!ccHeader.isBlank()) sb.append("Cc: ").append(ccHeader).append("\r\n");
            sb.append("Subject: ").append(encodeSubject(subject)).append("\r\n");
            sb.append("Date: ").append(java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)).append("\r\n");
            sb.append("MIME-Version: 1.0\r\n");
            sb.append("Content-Type: multipart/alternative; boundary=\"").append(boundary).append("\"\r\n");
            sb.append("\r\n");
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Type: text/plain; charset=UTF-8\r\n");
            sb.append("Content-Transfer-Encoding: base64\r\n\r\n");
            sb.append(encodeMimeBase64(plainBody)).append("\r\n");
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Type: text/html; charset=UTF-8\r\n");
            sb.append("Content-Transfer-Encoding: base64\r\n\r\n");
            sb.append(encodeMimeBase64(htmlBody)).append("\r\n");
            sb.append("--").append(boundary).append("--\r\n");
            sb.append(".\r\n");
            return sb.toString();
        }

        private static String encodeSubject(String subject) {
            return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(safe(subject).getBytes(StandardCharsets.UTF_8)) + "?=";
        }

        private static String encodeMimeBase64(String text) {
            return Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(safe(text).getBytes(StandardCharsets.UTF_8));
        }
        static class SmtpConnection {
            private final EmailConfig cfg;
            private Socket socket;
            private BufferedReader in;
            private BufferedWriter out;

            SmtpConnection(EmailConfig cfg) { this.cfg = cfg; }

            void connect() throws Exception {
                if ("SSL".equalsIgnoreCase(cfg.seguranca)) {
                    socket = SSLSocketFactory.getDefault().createSocket(cfg.smtpHost, cfg.porta);
                    ((SSLSocket) socket).startHandshake();
                } else {
                    socket = new Socket(cfg.smtpHost, cfg.porta);
                }
                socket.setSoTimeout(30000);
                setupStreams();
                expect(220);
            }

            void startTls() throws Exception {
                socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, cfg.smtpHost, cfg.porta, true);
                ((SSLSocket) socket).startHandshake();
                setupStreams();
            }

            private void setupStreams() throws IOException {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            }

            void send(String command, int... okCodes) throws IOException {
                out.write(command + "\r\n");
                out.flush();
                expect(okCodes);
            }

            void writeData(String data) throws IOException {
                out.write(data);
                out.flush();
            }

            void expect(int... okCodes) throws IOException {
                SmtpResponse response = readResponse();
                for (int ok : okCodes) if (response.code == ok) return;
                throw new IOException("SMTP " + response.code + ": " + response.message);
            }

            private SmtpResponse readResponse() throws IOException {
                String line = in.readLine();
                if (line == null) throw new EOFException("Servidor SMTP fechou a conexão.");
                StringBuilder msg = new StringBuilder(line);
                int code = parseInt(line.length() >= 3 ? line.substring(0,3) : "0", 0);
                while (line.length() > 3 && line.charAt(3) == '-') {
                    line = in.readLine();
                    if (line == null) break;
                    msg.append("\n").append(line);
                }
                return new SmtpResponse(code, msg.toString());
            }

            void close() {
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            }
        }

        record SmtpResponse(int code, String message) {}
    }

    static class SolicitarTonerDialog extends JDialog {
        private final Storage storage;
        private final JComboBox<String> modeloToner;
        private final JLabel coresLabel = new JLabel("Cor(es) do toner");
        private final JPanel coresPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        private final JCheckBox corCiano = new JCheckBox("Ciano");
        private final JCheckBox corMagenta = new JCheckBox("Magenta");
        private final JCheckBox corAmarelo = new JCheckBox("Amarelo");
        private final JCheckBox corPreto = new JCheckBox("Preto");
        private final JTextField numeroChamado = new JTextField();
        private final JTextField contador = new JTextField();
        private final JComboBox<String> solicitante = new JComboBox<>(new String[]{"", "USUÁRIO 1", "USUÁRIO 2", "USUÁRIO 3"});
        private final JTextField assunto = new JTextField("Solicitação de toner");
        private final JTextField anexoNome = new JTextField();
        private File anexoSelecionado;
        private SwingWorker<String, Void> contadorWorker;
        private JButton enviar;
        private boolean pedidoRegistrado = false;

        SolicitarTonerDialog(Frame owner, Storage storage, List<String> modelos) {
            super(owner, "Solicitar Toner", true);
            this.storage = storage;
            modeloToner = new JComboBox<>(modelos.toArray(new String[0]));
            modeloToner.setEditable(true);
            modeloToner.setSelectedItem("");
            setSize(520, 500);
            setLocationRelativeTo(owner);
            ((AbstractDocument) contador.getDocument()).setDocumentFilter(new NumericDocumentFilter());
            build();
            configurarPreenchimentoAutomaticoContador();
            configurarAssuntoAutomatico();
        }

        private void build() {
            JPanel root = new JPanel(new BorderLayout(10,10));
            root.setBorder(new EmptyBorder(12,12,12,12));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(4,4,4,4);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;

            JLabel aviso = new JLabel("O pedido será enviado para o fluxo configurado no Power Automate.");
            aviso.setForeground(new Color(70,70,70));
            form.add(aviso, c); c.gridy++;

            addField(form, c, "Número do chamado", numeroChamado);
            addComboField(form, c, "Modelo / descrição do toner", modeloToner);
            coresPanel.setOpaque(false);
            coresPanel.add(corCiano);
            coresPanel.add(corMagenta);
            coresPanel.add(corAmarelo);
            coresPanel.add(corPreto);
            form.add(coresLabel, c); c.gridy++;
            form.add(coresPanel, c); c.gridy++;
            atualizarCampoCoresToner();
            addField(form, c, "Contador", contador);
            addComboField(form, c, "Solicitante", solicitante);
            addField(form, c, "Assunto", assunto);

            JLabel anexoLabel = new JLabel("Anexo (imagem ou arquivo)");
            form.add(anexoLabel, c); c.gridy++;
            JPanel anexoPanel = new JPanel(new BorderLayout(6, 0));
            anexoNome.setEditable(false);
            anexoNome.setFont(anexoNome.getFont().deriveFont(15f));
            JButton selecionarAnexo = new JButton("Selecionar...");
            JButton removerAnexo = new JButton("Remover");
            selecionarAnexo.addActionListener(e -> escolherAnexo());
            removerAnexo.addActionListener(e -> removerAnexo());
            JPanel anexoButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            anexoButtons.add(selecionarAnexo);
            anexoButtons.add(removerAnexo);
            anexoPanel.add(anexoNome, BorderLayout.CENTER);
            anexoPanel.add(anexoButtons, BorderLayout.EAST);
            form.add(anexoPanel, c); c.gridy++;
            JLabel dicaAnexo = new JLabel("Opcional. O arquivo será enviado como anexo real pelo Power Automate. Limite: 10 MB.");
            dicaAnexo.setForeground(new Color(90, 90, 90));
            dicaAnexo.setFont(dicaAnexo.getFont().deriveFont(11f));
            form.add(dicaAnexo, c); c.gridy++;

            root.add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelar = new JButton("Cancelar");
            enviar = new JButton("Enviar pedido");
            enviar.setBackground(new Color(245,230,0));
            enviar.setForeground(Color.BLACK);
            cancelar.addActionListener(e -> dispose());
            enviar.addActionListener(e -> enviarEmail());
            buttons.add(cancelar);
            buttons.add(enviar);
            root.add(buttons, BorderLayout.SOUTH);
            setContentPane(root);
        }

        private void configurarAssuntoAutomatico() {
            DocumentListener listener = new DocumentListener() {
                private void atualizar() {
                    atualizarAssuntoComChamado();
                }
                @Override public void insertUpdate(DocumentEvent e) { atualizar(); }
                @Override public void removeUpdate(DocumentEvent e) { atualizar(); }
                @Override public void changedUpdate(DocumentEvent e) { atualizar(); }
            };
            numeroChamado.getDocument().addDocumentListener(listener);
            atualizarAssuntoComChamado();
        }

        private void atualizarAssuntoComChamado() {
            String chamado = numeroChamado.getText().trim();
            if (chamado.isBlank()) {
                assunto.setText("Solicitação de toner");
            } else {
                assunto.setText("Solicitação de toner - " + chamado);
            }
        }

        private void addField(JPanel form, GridBagConstraints c, String label, JTextField field) {
            JLabel l = new JLabel(label);
            form.add(l, c); c.gridy++;
            field.setFont(field.getFont().deriveFont(15f));
            form.add(field, c); c.gridy++;
        }

        private void addComboField(JPanel form, GridBagConstraints c, String label, JComboBox<String> combo) {
            JLabel l = new JLabel(label);
            form.add(l, c); c.gridy++;
            combo.setFont(combo.getFont().deriveFont(15f));
            Component editor = combo.getEditor().getEditorComponent();
            if (editor instanceof JTextField tf) tf.setFont(tf.getFont().deriveFont(15f));
            form.add(combo, c); c.gridy++;
        }

        private void configurarPreenchimentoAutomaticoContador() {
            ActionListener atualizar = e -> SwingUtilities.invokeLater(() -> {
                atualizarCampoCoresToner();
                preencherContadorAutomaticamente();
            });
            modeloToner.addActionListener(atualizar);
            modeloToner.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    SwingUtilities.invokeLater(() -> {
                        atualizarCampoCoresToner();
                        preencherContadorAutomaticamente();
                    });
                }
            });
            Component editor = modeloToner.getEditor().getEditorComponent();
            if (editor instanceof JTextField tf) {
                tf.addActionListener(atualizar);
                tf.addFocusListener(new FocusAdapter() {
                    @Override public void focusLost(FocusEvent e) {
                        atualizarCampoCoresToner();
                        preencherContadorAutomaticamente();
                    }
                });
            }
        }

        private void atualizarCampoCoresToner() {
            boolean mostrar = isImpressoraColorida(obterImpressoraSelecionada());
            coresLabel.setVisible(mostrar);
            coresPanel.setVisible(mostrar);
            if (!mostrar) {
                corCiano.setSelected(false);
                corMagenta.setSelected(false);
                corAmarelo.setSelected(false);
                corPreto.setSelected(false);
            }
            revalidate();
            repaint();
        }

        private boolean isImpressoraColorida(String impressora) {
            String n = normalizarImpressora(impressora);
            return n.contains("TONER COLORIDO")
                    || n.contains("RICOH MP C307")
                    || n.contains("SAMSUNG C3060FR");
        }

        private String obterCoresSelecionadas() {
            List<String> cores = new ArrayList<>();
            if (corCiano.isSelected()) cores.add("Ciano");
            if (corMagenta.isSelected()) cores.add("Magenta");
            if (corAmarelo.isSelected()) cores.add("Amarelo");
            if (corPreto.isSelected()) cores.add("Preto");
            return String.join(", ", cores);
        }

        private String aplicarCoresAoToner(String tonerBase, String cores) {
            if (cores == null || cores.isBlank()) return tonerBase;
            String marcador = cores.contains(",") ? " - Cores: " : " - Cor: ";
            return tonerBase + marcador + cores;
        }

        private void preencherContadorAutomaticamente() {
            String impressora = obterImpressoraSelecionada();
            if (impressora.isBlank()) return;
            String impressoraConsulta = resolverAliasImpressora(impressora);

            String contadorHistorico = buscarContadorPorImpressora(impressoraConsulta);
            if (!contadorHistorico.isBlank()) {
                contador.setText(contadorHistorico);
            }

            // A versão acadêmica não consulta equipamentos reais na rede.
            if (ACADEMIC_DEMO_MODE) return;

            String ip = extrairIp(impressoraConsulta);
            if (ip.isBlank()) return;

            if (contadorWorker != null && !contadorWorker.isDone()) {
                contadorWorker.cancel(true);
            }

            contadorWorker = new SwingWorker<>() {
                @Override protected String doInBackground() {
                    return consultarContadorSnmp(ip);
                }
                @Override protected void done() {
                    try {
                        if (isCancelled()) return;
                        String valor = get();
                        String atual = resolverAliasImpressora(obterImpressoraSelecionada());
                        if (!valor.isBlank() && extrairIp(atual).equals(ip)) {
                            contador.setText(valor);
                        }
                    } catch (Exception ignored) {}
                }
            };
            contadorWorker.execute();
        }

        private String obterImpressoraSelecionada() {
            String valor = safe(Objects.toString(modeloToner.getSelectedItem(), "")).trim();
            if (valor.isBlank()) {
                valor = safe(Objects.toString(modeloToner.getEditor().getItem(), "")).trim();
            }
            return valor;
        }

        private String resolverAliasImpressora(String impressora) {
            return impressora;
        }

        private String buscarContadorPorImpressora(String impressora) {
            String ipSelecionado = extrairIp(impressora);
            List<Pedido> pedidos = storage.loadPedidos();
            for (int i = pedidos.size() - 1; i >= 0; i--) {
                Pedido p = pedidos.get(i);
                if (matchesImpressora(impressora, p.departamento, ipSelecionado) || matchesImpressora(impressora, p.corToner, ipSelecionado)) {
                    String cont = safe(p.contador).replaceAll("[^0-9]", "").trim();
                    if (!cont.isBlank()) return cont;
                }
            }
            return "";
        }

        private boolean matchesImpressora(String selecionada, String cadastrada, String ipSelecionado) {
            String selecionadaAlias = resolverAliasImpressora(selecionada);
            String cadastradaAlias = resolverAliasImpressora(cadastrada);
            String b = normalizarImpressora(cadastradaAlias);
            if (b.isBlank()) return false;
            String ipCadastrado = extrairIp(cadastradaAlias);
            if (!ipSelecionado.isBlank() && !ipCadastrado.isBlank() && ipSelecionado.equals(ipCadastrado)) return true;
            String a = normalizarImpressora(selecionadaAlias);
            if (a.isBlank()) return false;
            return a.equals(b) || a.contains(b) || b.contains(a);
        }

        private String normalizarImpressora(String valor) {
            String s = safe(valor).toUpperCase(Locale.ROOT).trim();
            s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            return s.replaceAll("[^A-Z0-9]+", " ").replaceAll("\\s+", " ").trim();
        }

        private String extrairIp(String texto) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)")
                    .matcher(safe(texto));
            return m.find() ? m.group(1) : "";
        }

        private String consultarContadorSnmp(String ip) {
            String[] oids = {
                    "1.3.6.1.2.1.43.10.2.1.4.1.1",
                    "1.3.6.1.2.1.43.10.2.1.4.1.0",
                    "1.3.6.1.2.1.43.10.2.1.5.1.1"
            };
            for (String oid : oids) {
                try {
                    String valor = snmpGetInteger(ip, "public", oid, 1200);
                    if (!valor.isBlank()) return valor;
                } catch (Exception ignored) {}
            }
            return "";
        }

        private String snmpGetInteger(String ip, String community, String oid, int timeoutMs) throws IOException {
            byte[] oidBytes = encodeOid(oid);
            byte[] request = buildSnmpGetRequest(community, oidBytes);
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(timeoutMs);
                DatagramPacket packet = new DatagramPacket(request, request.length, InetAddress.getByName(ip), 161);
                socket.send(packet);
                byte[] buffer = new byte[4096];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);
                byte[] data = Arrays.copyOf(response.getData(), response.getLength());
                return extractSnmpIntegerValue(data, oidBytes);
            }
        }

        private byte[] buildSnmpGetRequest(String community, byte[] oidBytes) throws IOException {
            byte[] requestId = encodeInteger(1);
            byte[] errorStatus = encodeInteger(0);
            byte[] errorIndex = encodeInteger(0);
            byte[] varBind = encodeSequence(concat(encodeTlv(0x06, oidBytes), encodeTlv(0x05, new byte[0])));
            byte[] varBindList = encodeSequence(varBind);
            byte[] pdu = encodeTlv(0xA0, concat(requestId, errorStatus, errorIndex, varBindList));
            byte[] version = encodeInteger(0);
            byte[] communityBytes = encodeTlv(0x04, community.getBytes(StandardCharsets.US_ASCII));
            return encodeSequence(concat(version, communityBytes, pdu));
        }

        private String extractSnmpIntegerValue(byte[] data, byte[] oidBytes) {
            int idx = indexOf(data, oidBytes);
            if (idx < 0) return "";
            int pos = idx + oidBytes.length;
            while (pos < data.length && (data[pos] & 0xFF) == 0x05) { // NULL, caso apareça
                pos++;
                int[] len = readBerLength(data, pos);
                if (len == null) return "";
                pos = len[1] + len[0];
            }
            if (pos >= data.length) return "";
            int type = data[pos++] & 0xFF;
            int[] len = readBerLength(data, pos);
            if (len == null) return "";
            int length = len[0];
            pos = len[1];
            if (pos + length > data.length) return "";
            if (type == 0x02 || type == 0x41 || type == 0x42 || type == 0x43 || type == 0x46) {
                long value = 0;
                for (int i = 0; i < length; i++) {
                    value = (value << 8) | (data[pos + i] & 0xFFL);
                }
                return String.valueOf(value);
            }
            return "";
        }

        private byte[] encodeOid(String oid) {
            String[] parts = oid.split("\\.");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            out.write(first * 40 + second);
            for (int i = 2; i < parts.length; i++) {
                long value = Long.parseLong(parts[i]);
                byte[] encoded = encodeBase128(value);
                out.writeBytes(encoded);
            }
            return out.toByteArray();
        }

        private byte[] encodeBase128(long value) {
            byte[] tmp = new byte[10];
            int pos = tmp.length;
            tmp[--pos] = (byte)(value & 0x7F);
            value >>= 7;
            while (value > 0) {
                tmp[--pos] = (byte)((value & 0x7F) | 0x80);
                value >>= 7;
            }
            return Arrays.copyOfRange(tmp, pos, tmp.length);
        }

        private byte[] encodeInteger(int value) throws IOException {
            return encodeTlv(0x02, new byte[]{(byte)value});
        }

        private byte[] encodeSequence(byte[] value) throws IOException {
            return encodeTlv(0x30, value);
        }

        private byte[] encodeTlv(int tag, byte[] value) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(tag);
            writeBerLength(out, value.length);
            out.write(value);
            return out.toByteArray();
        }

        private void writeBerLength(ByteArrayOutputStream out, int length) {
            if (length < 128) {
                out.write(length);
            } else if (length <= 255) {
                out.write(0x81);
                out.write(length);
            } else {
                out.write(0x82);
                out.write((length >> 8) & 0xFF);
                out.write(length & 0xFF);
            }
        }

        private int[] readBerLength(byte[] data, int pos) {
            if (pos >= data.length) return null;
            int first = data[pos++] & 0xFF;
            if ((first & 0x80) == 0) return new int[]{first, pos};
            int count = first & 0x7F;
            if (count == 0 || count > 4 || pos + count > data.length) return null;
            int value = 0;
            for (int i = 0; i < count; i++) value = (value << 8) | (data[pos++] & 0xFF);
            return new int[]{value, pos};
        }

        private byte[] concat(byte[]... arrays) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] a : arrays) out.write(a);
            return out.toByteArray();
        }

        private int indexOf(byte[] data, byte[] pattern) {
            outer:
            for (int i = 0; i <= data.length - pattern.length; i++) {
                for (int j = 0; j < pattern.length; j++) {
                    if (data[i + j] != pattern[j]) continue outer;
                }
                return i;
            }
            return -1;
        }

        private void escolherAnexo() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Selecionar imagem ou arquivo");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file != null) {
                    long max = AttachmentData.MAX_BYTES;
                    if (file.length() > max) {
                        JOptionPane.showMessageDialog(this,
                                "O arquivo selecionado é maior que 10 MB. Selecione um arquivo menor.",
                                "Arquivo muito grande", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    anexoSelecionado = file;
                    anexoNome.setText(file.getName());
                    anexoNome.setToolTipText(file.getAbsolutePath());
                }
            }
        }

        private void removerAnexo() {
            anexoSelecionado = null;
            anexoNome.setText("");
            anexoNome.setToolTipText(null);
        }

        private void registrarPedidoNaLista(String chamadoTexto, String impressora, String contadorTexto, String solicitanteTexto, String obs) {
            if (pedidoRegistrado) return;
            Pedido novo = new Pedido();
            novo.chamado = chamadoTexto;
            novo.departamento = impressora;
            novo.contador = contadorTexto;
            novo.solicitante = solicitanteTexto;
            novo.nota = obs;
            novo.corToner = impressora;
            novo.status = Pedido.STATUS_AGUARDANDO;
            novo.dataSolicitacao = LocalDateTime.now().toString();
            List<Pedido> atuais = storage.loadPedidos();
            atuais.add(novo);
            storage.savePedidos(atuais);
            pedidoRegistrado = true;
        }

        private void enviarEmail() {
            EmailConfig cfg = storage.loadEmailConfig();
            if (!cfg.isComplete()) {
                JOptionPane.showMessageDialog(this,
                        "Configure a URL do Power Automate e o e-mail do fornecedor antes de enviar.");
                EmailConfigDialog d = new EmailConfigDialog((Frame) getOwner(), storage);
                d.setVisible(true);
                return;
            }

            String chamadoTexto = numeroChamado.getText().trim();
            String tonerBase = safe(Objects.toString(modeloToner.getEditor().getItem(), "")).trim();
            String coresSelecionadas = obterCoresSelecionadas();
            String toner = aplicarCoresAoToner(tonerBase, coresSelecionadas);
            String contadorTexto = contador.getText().trim();
            String solicitanteTexto = safe(Objects.toString(solicitante.getSelectedItem(), "")).trim();
            String subj = assunto.getText().trim();
            AttachmentData anexo;
            try {
                anexo = AttachmentData.fromFile(anexoSelecionado);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro no anexo", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String obs = anexo == null ? "" : "Anexo: " + anexo.nome;

            if (tonerBase.isBlank()) {
                JOptionPane.showMessageDialog(this, "Informe o toner que deseja solicitar.");
                return;
            }
            if (isImpressoraColorida(tonerBase) && coresSelecionadas.isBlank()) {
                JOptionPane.showMessageDialog(this, "Selecione pelo menos uma cor do toner.");
                return;
            }
            if (!chamadoTexto.isBlank() && (subj.isBlank() || subj.equalsIgnoreCase("Solicitação de toner"))) {
                subj = "Solicitação de toner - " + chamadoTexto;
            }
            if (subj.isBlank()) subj = "Solicitação de toner";

            String plainBody = "Olá,\n\n"
                    + "Gostaria de solicitar o seguinte toner:\n\n"
                    + (chamadoTexto.isBlank() ? "" : "Número do chamado: " + chamadoTexto + "\n")
                    + "Impressora: " + toner + "\n"
                    + (contadorTexto.isBlank() ? "" : "Contador: " + contadorTexto + "\n")
                    + (solicitanteTexto.isBlank() ? "" : "Solicitante: " + solicitanteTexto + "\n")
                    + "Status: Aguardando Aprovação\n"
                    + (anexo == null ? "" : "\nAnexo: " + anexo.nome + "\n")
                    + "\nAtenciosamente.";
            String htmlBody = buildSolicitacaoHtmlEmail(chamadoTexto, toner, contadorTexto, solicitanteTexto, "", anexo);

            enviar.setEnabled(false);
            enviar.setText("Enviando...");

            final String assuntoFinal = subj;
            new SwingWorker<EmailDispatchResult, Void>() {
                @Override protected EmailDispatchResult doInBackground() throws Exception {
                    PowerAutomateSender.send(cfg, assuntoFinal, plainBody, htmlBody,
                            chamadoTexto, toner, contadorTexto, solicitanteTexto, obs, anexo);
                    return new EmailDispatchResult(true, false,
                            ACADEMIC_DEMO_MODE
                                    ? "Modo demonstração: o envio foi simulado e nenhum dado foi enviado para fora do computador."
                                    : "Pedido enviado com sucesso para o Power Automate.\n\nO fluxo configurado será responsável por enviar o e-mail ao fornecedor: " + cfg.fornecedorEmail + ".");
                }
                @Override protected void done() {
                    enviar.setEnabled(true);
                    enviar.setText("Enviar pedido");
                    try {
                        EmailDispatchResult result = get();
                        if (result.sent || result.opened) {
                            registrarPedidoNaLista(chamadoTexto, toner, contadorTexto, solicitanteTexto, obs);
                        }
                        JOptionPane.showMessageDialog(SolicitarTonerDialog.this, result.message);
                        if (result.sent || result.opened) dispose();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(SolicitarTonerDialog.this,
                                "Não foi possível enviar ou abrir o e-mail.\n\n" + cause.getMessage(),
                                "Erro ao enviar", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }



    static class NumericDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;
            String digitsOnly = string.replaceAll("[^0-9]", "");
            if (!digitsOnly.isEmpty()) {
                super.insertString(fb, offset, digitsOnly, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) return;
            String digitsOnly = text.replaceAll("[^0-9]", "");
            super.replace(fb, offset, length, digitsOnly, attrs);
        }
    }

    static class EstoqueDialog extends JDialog {
        private final Storage storage;
        private final StockTableModel model;
        EstoqueDialog(Frame owner, Storage storage) {
            super(owner,"Estoque de Toners",true); this.storage=storage; this.model=new StockTableModel(storage.loadEstoque()); setSize(850,520); setLocationRelativeTo(owner); build();
        }
        private void build() {
            JTable table = new JTable(model); table.setRowHeight(26); table.setAutoCreateRowSorter(true); table.setDefaultRenderer(Object.class, new StockCellRenderer(model));
            JPanel root = new JPanel(new BorderLayout(8,8)); root.setBorder(new EmptyBorder(10,10,10,10));
            JLabel title = new JLabel("Estoque de Toners"); title.setFont(title.getFont().deriveFont(Font.BOLD,22f)); root.add(title,BorderLayout.NORTH); root.add(new JScrollPane(table),BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); JButton novo=new JButton("Novo"); JButton editar=new JButton("Editar"); JButton remover=new JButton("Remover"); JButton salvar=new JButton("Salvar e fechar");
            novo.addActionListener(e->{ TonerItem item = itemDialog(null); if(item!=null){ model.items.add(item); model.fireTableDataChanged(); }});
            editar.addActionListener(e->{ int r=table.getSelectedRow(); if(r>=0){ int m=table.convertRowIndexToModel(r); TonerItem edited=itemDialog(model.items.get(m)); if(edited!=null){ model.items.set(m,edited); model.fireTableRowsUpdated(m,m); } }});
            remover.addActionListener(e->{ int r=table.getSelectedRow(); if(r>=0 && JOptionPane.showConfirmDialog(this,"Remover item?","Confirmar",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){ model.items.remove(table.convertRowIndexToModel(r)); model.fireTableDataChanged(); }});
            salvar.addActionListener(e->{ storage.saveEstoque(model.items); dispose(); }); buttons.add(novo); buttons.add(editar); buttons.add(remover); buttons.add(salvar); root.add(buttons,BorderLayout.SOUTH); setContentPane(root);
        }
        private TonerItem itemDialog(TonerItem base) {
            JTextField modelo = new JTextField(base==null?"":base.modelo); JTextField cor = new JTextField(base==null?"":base.cor); JTextField comp = new JTextField(base==null?"":base.compatibilidade); JTextField qtd = new JTextField(base==null?"0":String.valueOf(base.quantidade)); JTextField min = new JTextField(base==null?"1":String.valueOf(base.minimo)); JTextField local = new JTextField(base==null?"Almoxarifado":base.local);
            JPanel p = new JPanel(new GridLayout(12,1,4,4)); p.add(new JLabel("Modelo do toner")); p.add(modelo); p.add(new JLabel("Cor")); p.add(cor); p.add(new JLabel("Compatibilidade / Impressoras")); p.add(comp); p.add(new JLabel("Quantidade")); p.add(qtd); p.add(new JLabel("Mínimo")); p.add(min); p.add(new JLabel("Local")); p.add(local);
            if (JOptionPane.showConfirmDialog(this,p,base==null?"Novo toner":"Editar toner",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
                TonerItem i = new TonerItem(); i.id = base==null? UUID.randomUUID().toString() : base.id; i.modelo=modelo.getText(); i.cor=cor.getText(); i.compatibilidade=comp.getText(); i.quantidade=parseInt(qtd.getText(),0); i.minimo=parseInt(min.getText(),1); i.local=local.getText(); return i;
            }
            return null;
        }
    }

    static class StockTableModel extends AbstractTableModel {
        List<TonerItem> items; String[] cols={"Modelo","Cor","Compatibilidade","Qtd","Mínimo","Local","Alerta"};
        StockTableModel(List<TonerItem> items){ this.items=items; }
        public int getRowCount(){ return items.size(); } public int getColumnCount(){ return cols.length; } public String getColumnName(int c){ return cols[c]; }
        public Object getValueAt(int r,int c){ TonerItem i=items.get(r); return switch(c){ case 0->i.modelo; case 1->i.cor; case 2->i.compatibilidade; case 3->i.quantidade; case 4->i.minimo; case 5->i.local; default-> i.quantidade<=i.minimo?"Estoque baixo":"OK"; }; }
    }
    static class StockCellRenderer extends DefaultTableCellRenderer { StockTableModel model; StockCellRenderer(StockTableModel m){model=m;} public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){ Component comp=super.getTableCellRendererComponent(t,v,s,f,r,c); int m=t.convertRowIndexToModel(r); TonerItem item=model.items.get(m); if(!s){ comp.setBackground(item.quantidade<=item.minimo? new Color(255,230,230): Color.WHITE); comp.setForeground(Color.BLACK);} return comp; } }

    static class Pedido {
        static final String STATUS_AGUARDANDO = "Aguardando Aprovação";
        static final String STATUS_CAMINHO = "A Caminho";
        static final String STATUS_ENTREGUE = "Entregue";
        String id = UUID.randomUUID().toString();
        String chamado="", departamento="", equipamento="", ip="", numeroSerie="", complemento="", contador="", corToner="", solicitante="", nota="";
        String dataSolicitacao = LocalDateTime.now().toString();
        String dataAprovacao="", notaFiscal="", tipoEnvio="", codigoRastreio="", dataRecebimento="", chaveNfe="", quemRecebeu="", dataCobrancaRetorno="";
        String status = STATUS_AGUARDANDO;
        LocalDateTime getDataSolicitacaoTime(){ try { return LocalDateTime.parse(dataSolicitacao); } catch(Exception e){ return LocalDateTime.now(); } }
        String searchText(){ return String.join(" ", chamado, departamento, equipamento, ip, numeroSerie, complemento, contador, corToner, solicitante, nota, notaFiscal, tipoEnvio, codigoRastreio, dataCobrancaRetorno, status); }
        void copyFrom(Pedido o){ this.chamado=o.chamado; this.departamento=o.departamento; this.equipamento=o.equipamento; this.ip=o.ip; this.numeroSerie=o.numeroSerie; this.complemento=o.complemento; this.contador=o.contador; this.corToner=o.corToner; this.solicitante=o.solicitante; this.nota=o.nota; this.dataSolicitacao=o.dataSolicitacao; this.dataAprovacao=o.dataAprovacao; this.notaFiscal=o.notaFiscal; this.tipoEnvio=o.tipoEnvio; this.codigoRastreio=o.codigoRastreio; this.dataRecebimento=o.dataRecebimento; this.chaveNfe=o.chaveNfe; this.quemRecebeu=o.quemRecebeu; this.status=o.status; this.dataCobrancaRetorno=o.dataCobrancaRetorno; }
    }
    static class TonerItem { String id=UUID.randomUUID().toString(), modelo="", cor="", compatibilidade="", local=""; int quantidade=0, minimo=1; }

    static class Storage {
        final Path dataDir = Paths.get("dados-academico"); final Path pedidosFile=dataDir.resolve("pedidos.db"); final Path estoqueFile=dataDir.resolve("estoque.db"); final Path emailConfigFile=dataDir.resolve("email.properties");
        void ensureSampleData(){ try { Files.createDirectories(dataDir); if(!Files.exists(pedidosFile)) savePedidos(samplePedidos()); if(!Files.exists(estoqueFile)) saveEstoque(sampleEstoque()); } catch(Exception e){ throw new RuntimeException(e); } }
        List<Pedido> loadPedidos(){ ensureDir(); if(!Files.exists(pedidosFile)) return new ArrayList<>(); try { List<Pedido> out=new ArrayList<>(); for(String line: Files.readAllLines(pedidosFile,StandardCharsets.UTF_8)){ if(line.isBlank()) continue; String[] f=line.split("\\t",-1); Pedido p=new Pedido(); int i=0; p.id=d(f,i++); p.chamado=d(f,i++); p.departamento=d(f,i++); p.equipamento=d(f,i++); p.ip=d(f,i++); p.numeroSerie=d(f,i++); p.complemento=d(f,i++); p.contador=d(f,i++); p.corToner=d(f,i++); p.solicitante=d(f,i++); p.nota=d(f,i++); p.dataSolicitacao=d(f,i++); p.dataAprovacao=d(f,i++); p.notaFiscal=d(f,i++); p.tipoEnvio=d(f,i++); if (f.length >= 20) { p.codigoRastreio=d(f,i++); p.dataRecebimento=d(f,i++); p.chaveNfe=d(f,i++); p.quemRecebeu=d(f,i++); p.status=d(f,i++); if (i < f.length) p.dataCobrancaRetorno=d(f,i++); } else { p.dataRecebimento=d(f,i++); p.chaveNfe=d(f,i++); p.quemRecebeu=d(f,i++); p.status=d(f,i++); } if ("À Caminho".equals(p.status)) p.status=Pedido.STATUS_CAMINHO; if(p.status.isBlank()) p.status=Pedido.STATUS_AGUARDANDO; out.add(p);} return out; } catch(Exception e){ JOptionPane.showMessageDialog(null,"Erro ao carregar pedidos: "+e.getMessage()); return new ArrayList<>(); } }
        void savePedidos(List<Pedido> pedidos){ ensureDir(); List<String> lines=new ArrayList<>(); for(Pedido p:pedidos){ lines.add(String.join("\t", e(p.id),e(p.chamado),e(p.departamento),e(p.equipamento),e(p.ip),e(p.numeroSerie),e(p.complemento),e(p.contador),e(p.corToner),e(p.solicitante),e(p.nota),e(p.dataSolicitacao),e(p.dataAprovacao),e(p.notaFiscal),e(p.tipoEnvio),e(p.codigoRastreio),e(p.dataRecebimento),e(p.chaveNfe),e(p.quemRecebeu),e(p.status),e(p.dataCobrancaRetorno))); } try{ Files.write(pedidosFile,lines,StandardCharsets.UTF_8); } catch(Exception ex){ throw new RuntimeException(ex); } }
        List<TonerItem> loadEstoque(){ ensureDir(); if(!Files.exists(estoqueFile)) return new ArrayList<>(); try{ List<TonerItem> out=new ArrayList<>(); for(String line: Files.readAllLines(estoqueFile,StandardCharsets.UTF_8)){ if(line.isBlank()) continue; String[] f=line.split("\\t",-1); TonerItem i=new TonerItem(); int p=0; i.id=d(f,p++); i.modelo=d(f,p++); i.cor=d(f,p++); i.compatibilidade=d(f,p++); i.quantidade=parseInt(d(f,p++),0); i.minimo=parseInt(d(f,p++),1); i.local=d(f,p++); out.add(i);} return out; } catch(Exception e){ return new ArrayList<>(); } }
        void saveEstoque(List<TonerItem> items){ ensureDir(); List<String> lines=new ArrayList<>(); for(TonerItem i:items){ lines.add(String.join("\t",e(i.id),e(i.modelo),e(i.cor),e(i.compatibilidade),e(String.valueOf(i.quantidade)),e(String.valueOf(i.minimo)),e(i.local))); } try{ Files.write(estoqueFile,lines,StandardCharsets.UTF_8); } catch(Exception ex){ throw new RuntimeException(ex); } }
        void exportPedidosCsv(List<Pedido> pedidos, Path path) throws IOException { List<String> lines=new ArrayList<>(); lines.add("Chamado;Status;Departamento;Equipamento;IP;Numero Serie;Contador;Toner;Solicitante;Data Solicitacao;Data Aprovacao;NFe;Tipo Envio;Codigo Rastreio;Data Recebimento;Quem Recebeu;Nota;Retorno Cobrado Em"); for(Pedido p:pedidos){ lines.add(csv(p.chamado)+";"+csv(p.status)+";"+csv(p.departamento)+";"+csv(p.equipamento)+";"+csv(p.ip)+";"+csv(p.numeroSerie)+";"+csv(p.contador)+";"+csv(p.corToner)+";"+csv(p.solicitante)+";"+csv(p.getDataSolicitacaoTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))+";"+csv(p.dataAprovacao)+";"+csv(p.notaFiscal)+";"+csv(p.tipoEnvio)+";"+csv(p.codigoRastreio)+";"+csv(p.dataRecebimento)+";"+csv(p.quemRecebeu)+";"+csv(p.nota)+";"+csv(formatDateTimeDisplay(p.dataCobrancaRetorno))); } Files.write(path,lines,StandardCharsets.UTF_8); }

        EmailConfig loadEmailConfig() {
            ensureDir();
            EmailConfig cfg = new EmailConfig();
            if (!Files.exists(emailConfigFile)) return cfg;
            try (InputStream in = Files.newInputStream(emailConfigFile)) {
                Properties p = new Properties();
                p.load(in);
                cfg.modoEnvio = EmailConfig.MODO_POWER_AUTOMATE;
                cfg.powerAutomateUrl = p.getProperty("powerAutomateUrl", "");
                cfg.remetente = p.getProperty("remetente", "");
                cfg.senha = decodePassword(p.getProperty("senha", ""));
                cfg.smtpHost = p.getProperty("smtpHost", "smtp.example.com");
                cfg.porta = parseInt(p.getProperty("porta", "587"), 587);
                cfg.seguranca = p.getProperty("seguranca", "STARTTLS");
                cfg.fornecedorEmail = p.getProperty("fornecedorEmail", "");
                cfg.copiaEmail = p.getProperty("copiaEmail", "");
            } catch (Exception ignored) {}
            return cfg;
        }

        void saveEmailConfig(EmailConfig cfg) {
            ensureDir();
            Properties p = new Properties();
            p.setProperty("modoEnvio", EmailConfig.MODO_POWER_AUTOMATE);
            p.setProperty("powerAutomateUrl", safe(cfg.powerAutomateUrl));
            p.setProperty("remetente", safe(cfg.remetente));
            p.setProperty("senha", encodePassword(cfg.senha));
            p.setProperty("smtpHost", safe(cfg.smtpHost));
            p.setProperty("porta", String.valueOf(cfg.porta));
            p.setProperty("seguranca", safe(cfg.seguranca));
            p.setProperty("fornecedorEmail", safe(cfg.fornecedorEmail));
            p.setProperty("copiaEmail", safe(cfg.copiaEmail));
            try (OutputStream out = Files.newOutputStream(emailConfigFile)) {
                p.store(out, "Configuração de e-mail da Gestão de Toner");
            } catch (Exception ex) { throw new RuntimeException(ex); }
        }

        private static String encodePassword(String s) { return Base64.getEncoder().encodeToString(safe(s).getBytes(StandardCharsets.UTF_8)); }
        private static String decodePassword(String s) { try { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); } catch (Exception e) { return ""; } }
        private void ensureDir(){ try{ Files.createDirectories(dataDir); }catch(Exception e){ throw new RuntimeException(e);} }
        private static String e(String s){ return Base64.getEncoder().encodeToString(safe(s).getBytes(StandardCharsets.UTF_8)); }
        private static String d(String[] f,int i){ if(i>=f.length||f[i].isBlank()) return ""; try{return new String(Base64.getDecoder().decode(f[i]),StandardCharsets.UTF_8);}catch(Exception ex){return "";} }
        private static String csv(String s){ return "\""+safe(s).replace("\"","\"\"")+"\""; }
        private List<Pedido> samplePedidos(){
            List<Pedido> l = new ArrayList<>();

            Pedido p1 = new Pedido();
            p1.chamado = "DEMO-2026-001";
            p1.departamento = "ADMINISTRATIVO - RICOH IM 430 - DEMO-001 - TONER W10";
            p1.corToner = p1.departamento;
            p1.contador = "12450";
            p1.solicitante = "USUÁRIO 1";
            p1.status = Pedido.STATUS_AGUARDANDO;
            p1.dataSolicitacao = "2026-09-01T09:15:00";
            l.add(p1);

            Pedido p2 = new Pedido();
            p2.chamado = "DEMO-2026-002";
            p2.departamento = "FINANCEIRO - RICOH MP C307 - DEMO-003 - TONER COLORIDO - Cor: Ciano";
            p2.corToner = p2.departamento;
            p2.contador = "4590";
            p2.solicitante = "USUÁRIO 2";
            p2.notaFiscal = "NF-DEMO-002";
            p2.tipoEnvio = "Correio";
            p2.codigoRastreio = "RASTREAMENTO-DEMO-002";
            p2.status = Pedido.STATUS_CAMINHO;
            p2.dataSolicitacao = "2026-09-03T14:30:00";
            p2.dataAprovacao = "04/09/2026";
            l.add(p2);

            Pedido p3 = new Pedido();
            p3.chamado = "DEMO-2026-003";
            p3.departamento = "RECEPÇÃO - RICOH IM 430 - DEMO-002 - TONER W10";
            p3.corToner = p3.departamento;
            p3.contador = "8320";
            p3.solicitante = "USUÁRIO 3";
            p3.notaFiscal = "NF-DEMO-003";
            p3.tipoEnvio = "Técnico";
            p3.quemRecebeu = "USUÁRIO 1";
            p3.status = Pedido.STATUS_ENTREGUE;
            p3.dataSolicitacao = "2026-09-05T08:10:00";
            p3.dataRecebimento = "08/09/2026";
            l.add(p3);

            Pedido p4 = new Pedido();
            p4.chamado = "DEMO-2026-004";
            p4.departamento = "LOGÍSTICA - RICOH IM 430 - DEMO-005 - TONER CF2";
            p4.corToner = p4.departamento;
            p4.contador = "21870";
            p4.solicitante = "USUÁRIO 1";
            p4.status = Pedido.STATUS_AGUARDANDO;
            p4.dataSolicitacao = "2026-09-10T16:45:00";
            l.add(p4);

            return l;
        }
        private List<TonerItem> sampleEstoque(){
            List<TonerItem> l = new ArrayList<>();
            TonerItem a = new TonerItem();
            a.modelo = "TONER W10";
            a.cor = "Preto";
            a.compatibilidade = "Impressoras monocromáticas de demonstração";
            a.quantidade = 4;
            a.minimo = 2;
            a.local = "Estoque Demo";
            l.add(a);

            TonerItem b = new TonerItem();
            b.modelo = "TONER COLORIDO";
            b.cor = "Ciano / Magenta / Amarelo / Preto";
            b.compatibilidade = "Impressoras coloridas de demonstração";
            b.quantidade = 2;
            b.minimo = 1;
            b.local = "Estoque Demo";
            l.add(b);
            return l;
        }
    }

    static void styleDialogButton(JButton b, Color bg, Color fg) {
        b.setUI(new BasicButtonUI());
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setBorder(new CompoundBorder(new LineBorder(bg.darker()), new EmptyBorder(8,12,8,12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static String htmlEscape(String s){
        String value = safe(s);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '&') out.append("&amp;");
            else if (ch == '<') out.append("&lt;");
            else if (ch == '>') out.append("&gt;");
            else if (ch == '"') out.append("&quot;");
            else if (ch == '\'') out.append("&#39;");
            else if (ch > 127) out.append("&#").append((int) ch).append(';');
            else out.append(ch);
        }
        return out.toString();
    }
    static String nl2br(String s){ return htmlEscape(s).replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>"); }
    static String buildSolicitacaoHtmlEmail(String chamado, String impressora, String contador, String solicitante, String observacoes) {
        return buildSolicitacaoHtmlEmail(chamado, impressora, contador, solicitante, observacoes, null);
    }

    static String buildSolicitacaoHtmlEmail(String chamado, String impressora, String contador, String solicitante, String observacoes, AttachmentData anexo) {
        String obsRow = safe(observacoes).isBlank() ? "" : "<tr style=\"background:#292929;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Observa&#231;&#245;es</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + nl2br(observacoes) + "</td></tr>";
        String anexoRow = anexo == null ? "" : "<tr style=\"background:#292929;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Anexo</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(anexo.nome) + "</td></tr>";
        return "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><meta charset=\"UTF-8\"></head><body style=\"margin:0;padding:24px;background:#111111;font-family:Arial,'Segoe UI',Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:760px;border-collapse:collapse;background:#1f1f1f;border:1px solid #343434;\">"
                + "<tr><td colspan=\"2\" style=\"padding:14px 16px;background:#2b2b2b;color:#ff9f1a;font-size:30px;font-weight:700;\">Solicita&#231;&#227;o de Toner</td></tr>"
                + "<tr style=\"background:#262626;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">N&#250;mero do Chamado</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(chamado) + "</td></tr>"
                + "<tr style=\"background:#303030;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Impressora</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(impressora) + "</td></tr>"
                + "<tr style=\"background:#262626;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Contador</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(contador) + "</td></tr>"
                + "<tr style=\"background:#303030;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Solicitante</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(solicitante) + "</td></tr>"
                + "<tr style=\"background:#262626;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Status</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">Aguardando Aprova&#231;&#227;o</td></tr>"
                + anexoRow
                + obsRow
                + "</table></body></html>";
    }

    static String buildCobrancaHtmlEmail(String chamado, String impressora, String contador, String solicitante, String statusAtual) {
        return "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><meta charset=\"UTF-8\"></head><body style=\"margin:0;padding:24px;background:#111111;font-family:Arial,'Segoe UI',Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:760px;border-collapse:collapse;background:#1f1f1f;border:1px solid #343434;\">"
                + "<tr><td colspan=\"2\" style=\"padding:14px 16px;background:#2b2b2b;color:#4da3ff;font-size:30px;font-weight:700;\">Por gentileza analisar a solicitação abaixo</td></tr>"
                + "<tr style=\"background:#262626;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">N&#250;mero do Chamado</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(chamado) + "</td></tr>"
                + "<tr style=\"background:#303030;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Impressora</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(impressora) + "</td></tr>"
                + "<tr style=\"background:#262626;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Contador</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(contador) + "</td></tr>"
                + "<tr style=\"background:#303030;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Solicitante</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(solicitante) + "</td></tr>"
                + "<tr style=\"background:#262626;\"><td style=\"padding:14px 16px;color:#ffffff;font-weight:700;width:230px;border-top:1px solid #3b3b3b;\">Status</td><td style=\"padding:14px 16px;color:#ffffff;border-top:1px solid #3b3b3b;\">" + htmlEscape(statusAtual) + "</td></tr>"
                + "</table></body></html>";
    }

    static List<String> splitEmails(String raw) {
        List<String> out = new ArrayList<>();
        for (String part : safe(raw).split("[;,\\s]+")) {
            String email = part.trim();
            if (!email.isBlank() && !out.contains(email)) out.add(email);
        }
        return out;
    }

    static String joinEmails(String raw) {
        return String.join(", ", splitEmails(raw));
    }

    static String encodeMail(String s){ try { return URLEncoder.encode(safe(s), StandardCharsets.UTF_8).replace("+", "%20"); } catch(Exception e){ return safe(s); } }

    static String formatDateTimeDisplay(String value){
        String v = safe(value);
        if (v.isBlank()) return "";
        try { return LocalDateTime.parse(v).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); } catch(Exception ignored) {}
        return v;
    }
    static String safe(String s){ return s==null?"":s; }
    static Color statusColor(String s){ if(Pedido.STATUS_AGUARDANDO.equals(s)) return new Color(235,0,45); if(Pedido.STATUS_CAMINHO.equals(s)) return new Color(100,160,255); if(Pedido.STATUS_ENTREGUE.equals(s)) return new Color(120,210,70); return Color.WHITE; }
    static int parseInt(String s,int def){ try{return Integer.parseInt(s.trim());}catch(Exception e){return def;} }
    static String parseToIso(String s){ if(s==null||s.isBlank()) return LocalDateTime.now().toString(); for(DateTimeFormatter f: List.of(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),DateTimeFormatter.ofPattern("dd/MM/yyyy"))){ try{ if(f.toString().contains("HourOfDay")) return LocalDateTime.parse(s,f).toString(); return LocalDate.parse(s,f).atStartOfDay().toString(); }catch(DateTimeParseException ignored){} } try{return LocalDateTime.parse(s).toString();}catch(Exception e){return LocalDateTime.now().toString();} }
    static String shorten(String s,int max){ s=safe(s); return s.length()<=max?s:s.substring(0,Math.max(0,max-3))+"..."; }
    static void showError(String title, Exception ex){ JOptionPane.showMessageDialog(null, title+":\n"+ex.getMessage(),"Erro",JOptionPane.ERROR_MESSAGE); }
}
