package battleship;

/**
 * Classe criada para provocar uma violacao da convencao de nomes de parametros
 * (passo 4 da tarefa 2B, 122652).
 * Versao corrigida: o parametro passou a lowerCamelCase.
 */
public class ParameterNameDemo122652 {

    public int calcularPontos(int numTiros, int naviosAfundados) {
        return numTiros + naviosAfundados * 10;
    }
}
