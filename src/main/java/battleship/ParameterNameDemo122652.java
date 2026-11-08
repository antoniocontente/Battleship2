package battleship;

/**
 * Classe criada para provocar uma violacao da convencao de nomes de parametros
 * (passo 4 da tarefa 2B, 122652).
 */
public class ParameterNameDemo122652 {

    public int calcularPontos(int Num_Tiros, int naviosAfundados) {
        return Num_Tiros + naviosAfundados * 10;
    }
}
