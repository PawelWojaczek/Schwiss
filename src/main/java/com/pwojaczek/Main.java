package com.pwojaczek;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        MatchService matchService = new MatchService();
        matchService.initTournament();

        Scanner scanner = new Scanner(System.in);
        scanner.next();

    }
}
