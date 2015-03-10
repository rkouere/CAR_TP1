Creation d'un serveur ftp en Java
Congcong Xu - Nicolas Echallier
17/02/14

Introduction
Le but de ce Tp était de créer un server ftp permettant de gérer les commandes "de bases" d'un serveur ftp.
L'utilisateur est "nico" et le mot de passe est "password".
Le serveur marche à moitié dans filezilla (on peut se connecter mais on ne renvoit pas les bonnes donnée pour ls... donc le reste ne marche pas top non plus).
Toutes les commandes demandé marchent, c.a.d. :
- USER
- PASS
- RETR
- STOR
- LIST
- QUIT
- PWD
- CWD
- CDUP
Le mode passif marche aussi. Deux problemes avec le mode passif :
1. on ne peut plus revenir au mode actif.
2. on ne peut pas gérer deux connection passive en même temps.
La connection anonyme marche elle aussi.
Pour information : nous aurions probablement du gérer notre ouverture de port differement (tester que le port est ouvert et si ce n'est pas le cas, choisir un autre port). On aurait comme cela pu avoir plu

Architecture
Nous avons fait ce qui était demandé dans le TP, à savoir :
- Une classe "Serveur" qui écoute les demandes de connexion sur un port TCP (ici : 4000).
- Une classe FtpRequest comportant la gestion des commandes envoyés par le serveur.

Gestion d'erreurs
Nous avons géré les erreurs avec les exceptions par défault.

Code Samples
Il est demandé dans le rendu de présenter 5 codes samples interessant. Ce rendu de TP n'a pas implémenté de code avec soit une méthode contenant un algorithme intéressant, soit une liste de classes impliquées dans un design pattern soit une jolie optimisation. Je ne savais donc pas quoi mettre dans cette section.

Test
Nous avons essayé de faire des tests mais nous n'avons pas réussi à les effectuer. Nous arrivions à faire en sorte que les tests se "connecte" au client mais nous avions des problèmes de renvoit de message qui ne marchaient pas. Nous avons tout de même iclus le dossier de test.
