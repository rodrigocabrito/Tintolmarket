# Tintolmarket

Client-Server app for buying, selling and managing wines. 

## What can you do

Allows you to register in the app through an authentication process. Then, you can do various operations, such as add a Wine, sell it, inspect, buy some other Wine and rate them. There are also other functionallities related to user interaction such as communicating with others users and checking you balance.


## Functionalities

Once logged in, a menu will appear and you can choose witch operation to do. Here's an 101 of how they work:
- add `wine` `image` : Adds a wine to the app with the name `wine` and the image `image`.
- sell `wine` `value` `quantity` : Puts a wine with the name `wine` for sale, selling each bottle for `value`. `quantity` bottles will be put up for sale.
- view `wine` : Shows the info about `wine`, such as name, file image name, avg of ratings(1-5) and, if exists, any sale of that wine (seller, price for bottle and quantity).
- buy `wine` `seller` `quantity` : Buys `quantity` bottles of wine with name `wine` to the seller `seller`. 
- wallet : Shows the balance
- classify `wine` `stars` : Classifies the wine `wine` with `star`.
- talk `user` `message` : Sends a message `message` to user `user`.
- read : Shows all the messages received
- exit : Exits the app

## Limitations

Here are some of the operation limitations:
- add `wine` `image` : `wine` must be a String and `image` must be a String path to the file containing the image. It's imperative that the image      exists in the client_files directory, otherwise client is shutdown.
- sell `wine` `value` `quantity` : `wine` must be a String, while `value` and `quantity` both must be positive Integers. It's only possible to sell existing wines.
- view `wine` : `wine` must be a String and `wine` must exist.
- buy `wine` `seller` `quantity` : `wine` must be a String and `wine` must exist. `seller` must be a String and `seller` must be an existing user. Also `seller` cant be the user itself. `quantity` must be a positive Integer.
- classify `wine` `stars` : `wine` must be a String and `wine` must exist.
- talk `user` `message` : `user` must be a String and `user` must exist. `message` must be a String. `user` cannot be the user itself.


## Other explanations

File organization:

### data_bases
 
Contains the .txt files that store all data from the app.
- authentication.txt contains the credentials of every user in `clientID:passwd` format.
- balance.txt contains the balance of every registered user in `clientID;balance` format.
- chat.txt contains all sent messages that weren't yet read in `sender;receiver;msg` format.
- forSale.txt contains all sales from all users in `wineName;seller;price;quantity` format.
- wines.txt contains all added wines in format `wineName;imageFileName;[stars]` format.

These files are updated through the app execution, by structures in the server memory.

### client_files

Contains image files of the wines to be added. When view operation is called, the file that contains the image of the wine passed as arg in the view operation is overwritten.

### server_files

Contains image files of the wines added.

### Server memory (Local memory)

There are structures used to operate in the data bases. They are updated from the data_bases directory .txt files when the server starts.
- `ArrayList<Utilizador>` listaUts : Stores all registered users.
- `ArrayList<Wine>` listaWines : Stores all added wines.
- `HashMap<Utilizador,ArrayList<Sale>>` forSale : Stores all sales, mapping each list of sales to its seller (user).

## Other classes

- `Utilizador` is an object that represents an user, defined by a id `clientID` and a balance `balance`.
- `Wine` is and object that represents a wine, defined by a name `name`, image file name `image` and all ratings (1-5) `stars`.
- `Sale` is an object that represents a sale, defined by a wine `wine`, a price `value` and a quantity `quantity`.


## How to Run

To compile and run server:

```bash
java -jar TintolmarketServer.jar
```

To compile and run client:

```bash
java -jar Tintolmarket.jar <serverAddress> <userID> [password]
```

## 

Segurança e Confiabilidade @ Faculdade de Ciencias, University of Lisbon
