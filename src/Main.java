//package ArrayEstatico;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Main {
	
    private static final int maxContatos = 100;
    private static String[] listaContatos = new String[maxContatos + 1];
    private static int posicao = 0;
    private static int posicaoCarregar = 0; 
    private static String espera; 
    private static String tecla;
    private static String cpfLido, nomeLido, telefoneLido, cepLido;
    private static String path = System.getProperty("user.home");
    private static String filename = path + "/Agenda/" + "AgendaContatos.csv";
    private static boolean cheio = false;

    public static boolean carregarAgenda() {
        File file = new File(filename);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String linha;
                while ((linha = br.readLine()) != null && posicao < maxContatos + 1) {
                    if (linha.equals("") || linha.equals("\n") || linha.equals("\r") || linha.equals("\r\n") || linha.equals(" ")) {
                        listaContatos[posicao] = "";
                    } else {
                        listaContatos[posicao] = linha;
                    }
                    posicao++;
                }
            } catch (IOException e) {
                System.err.println("Erro ao carregar o arquivo: " + e.getMessage());
                return false;
            }

            cheio = true;
            for (int i = 1; i < listaContatos.length; i++) {
                if (listaContatos[i] == null || listaContatos[i].isEmpty()) {
                    cheio = false;
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    public static void salvarAgenda() {
        try {
            File dir = new File(path + "/Agenda/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
                for (int i = 0; i < posicao; i++) {
                    if (listaContatos[i] != null && !listaContatos[i].equals("")) {
                        bw.write(listaContatos[i]);
                        bw.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }

    public static int calculoBaseCpf(String cpf) {
        int soma = 0;
        int tamanhoCpf = cpf.length();
        int pesoInicial = tamanhoCpf + 1;

        for (int i = 0; i < tamanhoCpf; i++) {
            int digitoCpf = Character.getNumericValue(cpf.charAt(i));
            soma += digitoCpf * (pesoInicial - i);
        }

        int resto = soma % 11;
        if (resto < 2) {
            return 0;
        } else {
            return 11 - resto;
        }
    }

    public static boolean validarCpf(String cpf) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        int tamanhoCpf = cpfLimpo.length();

        if (tamanhoCpf != 11) {
            return false;
        }

        if (cpfLimpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        String primeirosNoveDigitos = cpfLimpo.substring(0, 9);
        String primeirosDezDigitos = cpfLimpo.substring(0, 10);
        int dv1 = Character.getNumericValue(cpfLimpo.charAt(9));
        int dv2 = Character.getNumericValue(cpfLimpo.charAt(10));

        int somaDigito1 = calculoBaseCpf(primeirosNoveDigitos);
        int somaDigito2 = calculoBaseCpf(primeirosDezDigitos);

        if (somaDigito1 != dv1) {
            return false;
        }
        if (somaDigito2 != dv2) {
            return false;
        }
        return true;
    }

    public static boolean existeContato(String cpf) {
        for (int i = 1; i < posicao; i++) {
            if (listaContatos[i] != null && !listaContatos[i].equals("")) {
                if (listaContatos[i].startsWith(cpf + ";")) {
                    return true;
                }
            }
        }
        return false;
    }
   
    public static String consultarCep(String cep) {
    	Scanner scanner = new Scanner(System.in);
        if (cep.length() != 8) {
            System.out.println("CEP inválido!");
            System.out.print("Digite o endereço manualmente: ");
            return scanner.nextLine();
        }
        try {
            URL url = new URL("https://viacep.com.br/ws/" + cep + "/json/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder resp = new StringBuilder();
            String linha;
            while ((linha = br.readLine()) != null) resp.append(linha);
            br.close();

            String json = resp.toString();
            if (json.contains("\"erro\"")) return "";
            String logradouro = json.split("\"logradouro\":")[1].split(",")[0].replace("\"", "").trim();
            String bairro = json.split("\"bairro\":")[1].split(",")[0].replace("\"", "").trim();
            String cidade = json.split("\"localidade\":")[1].split(",")[0].replace("\"", "").trim();
            String estado = json.split("\"uf\":")[1].split(",")[0].replace("\"", "").trim();
            return logradouro + " " + bairro + " " + cidade + " " + estado;
        } catch (Exception e) {
            return "Erro ao consultar CEP";
        }
    }

    
    private static String extrairValor(String json, String chave) {
        String chaveComAspas = "\"" + chave + "\":\"";
        int inicio = json.indexOf(chaveComAspas);
        if (inicio == -1) {
            return "";
        }
        inicio += chaveComAspas.length();
        int fim = json.indexOf("\"", inicio);
        if (fim == -1) {
            return "";
        }
        return json.substring(inicio, fim);
    }

    public static String criarContato(String cpf, String nome, String telefone, String endereco) {
        if (!existeContato(cpf)) {
            if (validarCpf(cpf)) {
                String contato = cpf + ";" + nome + ";" + telefone + ";" + endereco;
         
                for (int i = 1; i < listaContatos.length; i++) {
                    if (listaContatos[i] == null || listaContatos[i].isEmpty()) {
                        listaContatos[i] = contato;       
                        if (i >= posicao) {
                            posicao = i + 1;
                        }
                        salvarAgenda();
                        return "Contato cadastrado com sucesso!!";
                    }
                }

                if (posicao < listaContatos.length) {
                    listaContatos[posicao] = contato;
                    posicao++;
                    salvarAgenda();
                    return "Contato cadastrado com sucesso!!";
                } else {
                    return "Agenda Lotada!!";
                }
            } else {
                return "CPF INVÁLIDO";
            }
        } else {
            return "Contato já existe";
        }
    }

    public static String buscarContato(String cpf) {
        if (validarCpf(cpf)) {
            if (existeContato(cpf)) {
                for (int i = 1; i < posicao; i++) {
                    String contatoAtual = listaContatos[i];
                    if (contatoAtual != null && !contatoAtual.equals("")) {
                        String[] partes = contatoAtual.split(";");
                        if (partes.length > 0 && partes[0].equals(cpf)) {
                            return contatoAtual;
                        }
                    }
                }
            }
            return "Contato não encontrado";
        }
        return "CPF INVÁLIDO!!";
    }

    public static void EditarContato(String cpf) {
        Scanner scanner = new Scanner(System.in);
        int indice = -1;
        String Nome, Telefone, verifica;

        System.out.println("Digite o CPF do contato que deseja editar: ");
        verifica = scanner.nextLine();

        if (validarCpf(verifica)) {
            String contato = buscarContato(verifica);

            if (contato.equals("Contato não encontrado") || contato.equals("CPF INVÁLIDO!!")) {
                System.out.println(contato);
                System.out.println("\nPressione ENTER para continuar");
                scanner.nextLine();
                return;
            }

            limpa();
            System.out.println("Contato selecionado: \n" + contato);
            System.out.println("Você deseja EDITAR o contato? ");
            System.out.println("Confirme repetindo o CPF ou digite ENTER para CANCELAR: ");
            cpf = scanner.nextLine();
            
            if (cpf.equals(verifica)) {
                if (existeContato(cpf)) {
                  
                    for (int i = 1; i < posicao; i++) {
                        if (listaContatos[i] != null && !listaContatos[i].equals("")) {
                            String[] partes = listaContatos[i].split(";");
                            if (partes.length > 0 && partes[0].equals(cpf)) {
                                indice = i;
                                break;
                            }
                        }
                    }
                }

                if (validarCpf(cpf)) {
                    System.out.println("Digite o Nome: ");
                    Nome = scanner.nextLine();
                    System.out.println("Digite o Telefone para Contato: ");
                    Telefone = scanner.nextLine();
                    System.out.println("Digite o CEP: ");
                    cepLido = scanner.nextLine();
                    String Endereco = consultarCep(cepLido);
                    String NovoContato = cpf + ";" + Nome + ";" + Telefone + ";" + Endereco;

                    if (indice != -1) {
                        listaContatos[indice] = NovoContato;
                        salvarAgenda();
                    }
                    System.out.println("Contato editado com sucesso!");
                }
            } else if (cpf.equals("")) {
                System.out.println("Você cancelou a Edição!");
            } else {
                System.out.println("CPF INCOMPATÍVEL!");
            }
        } else {
            System.out.println("CPF INVÁLIDO!");
        }

        System.out.println("\nPressione ENTER para continuar");
        scanner.nextLine();
    }

    public static void listarContatos() {
        for (int i = 0; i < posicao; i++) {
            if (listaContatos[i] != null && !listaContatos[i].equals("")) {
                System.out.println(listaContatos[i]);
            }
        }
    }

    public static void excluirContato(String cpf) {
        Scanner scanner = new Scanner(System.in);
        String verifica = "";

        if (existeContato(cpf)) {
            int indice = -1;

            for (int i = 1; i < posicao; i++) {
                if (listaContatos[i] != null && !listaContatos[i].equals("")) {
                    String[] partes = listaContatos[i].split(";");
                    if (partes.length > 0 && partes[0].equals(cpf)) {
                        indice = i;
                        break;
                    }
                }
            }

            String contato = buscarContato(cpf);
            limpa();
            System.out.println("Contato selecionado: \n" + contato);
            System.out.println("Você deseja EXCLUIR o contato? ");
            System.out.println("Confirme repetindo o CPF ou digite ENTER para CANCELAR: ");
            verifica = scanner.nextLine();

            if (verifica.equals(cpf)) {
                if (indice != -1) {
                    listaContatos[indice] = "";
                    salvarAgenda();
                    cheio = false; 
                    System.out.println("O Contato foi excluído!");
                }
            } else if (verifica.equals("")) {
                System.out.println("Você decidiu CANCELAR! ");
            } else {
                System.out.println("INCOMPATÍVEL!");
            }
        } else {
            System.out.println("CONTATO INEXISTENTE!!");
        }
    }

   public static void teclaEnter() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Tecle ENTER para continuar");
        scanner.nextLine();
        limpa();
    }

    public static void limpa() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (final Exception e) {
            
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcao = -1;
        String busca;
       
        if (!carregarAgenda()) {
            listaContatos[0] = "CPF;Nome;Telefone;Endereço";
            posicao = 1;
            salvarAgenda();
        }

        do {
            System.out.println("-------AGENDA DE CONTATOS-------");
            System.out.println("1) Novo contato");
            System.out.println("2) Consultar contato");
            System.out.println("3) Editar contato");
            System.out.println("4) Excluir contato");
            System.out.println("5) Listar Contatos");
            System.out.println("6) Sair");
            System.out.println("--------------------------------");
            System.out.print("Digite a Opção Desejada: ");
            
            try {
                opcao = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                opcao = -1;
            }

            switch (opcao) {
                case 1:
                    limpa();
                    if (cheio) {
                        System.out.println("Agenda Lotada!!");
                        teclaEnter();
                    } else {
                        System.out.println("Cadastrar Novo Contato ");
                        System.out.println("Digite o CPF: ");
                        cpfLido = scanner.nextLine();

                        if (validarCpf(cpfLido)) {
                            if (existeContato(cpfLido)) {
                                System.out.println("Contato já existe!");
                                teclaEnter();
                            } else {
                                System.out.println("Digite o Nome: ");
                                nomeLido = scanner.nextLine();
                                System.out.println("Digite o Telefone para Contato: ");
                                telefoneLido = scanner.nextLine();
                                System.out.println("Digite o CEP: ");
                                cepLido = scanner.nextLine();

                                String endereco = consultarCep(cepLido);
                                System.out.println(criarContato(cpfLido, nomeLido, telefoneLido, endereco));
                                teclaEnter();
                            }
                        } else {
                                System.out.println("CPF INVÁLIDO!");
                                teclaEnter();
                        }
                    }
                    break;
                case 2:
                    limpa();
                    System.out.println("Buscar Contato ");
                    System.out.println("Digite o CPF do contato que deseja buscar: ");
                    busca = scanner.nextLine();
                    System.out.println(buscarContato(busca));
                    teclaEnter();
                    break;
                case 3:
                    limpa();
                    System.out.println("Editar Contato");
                    EditarContato("");
                    break;
                case 4:
                    limpa();
                    System.out.println("Excluir Contato");
                    System.out.println("Digite o CPF do contato que deseja excluir");
                    System.out.println("ou Digite ENTER para voltar ao MENU anterior: ");
                    busca = scanner.nextLine();
                    if (busca.equals("")) {
                        break;
                    } else {
                        excluirContato(busca);
                        teclaEnter();
                    }
                    break;
                case 5:
                    limpa();
                    System.out.println("Lista de Contatos");
                    listarContatos();
                    teclaEnter();
                    break;
                case 6:
                    salvarAgenda();
                    System.out.println("Programa finalizado!");
                    break;
                default:
                    System.out.println("Opção inválida!");
                    teclaEnter();
                    break;
            }
        } while (opcao != 6);
        scanner.close(); 
    }
}
