Projeto existente num repositorio git, onde é possivel uma melhor leitura do README (.md no git)

    https://github.com/rodrigocabrito/Tintolmarket



# Tintolmarket

Client-Server app for buying, selling and managing wines. 

## What can you do

Allows you to register in the app through an authentication process. Then, you can do various operations, such as add a Wine,
sell it, inspect, buy some other Wine and rate them. There are other functionallities related to user interaction such as
communicating with others users and checking you balance.
All the operations are safe and verified by the server to check the client's identity, including the authentication. In the chat,
only the clients in a conversation can read the content of each message.


## Functionalities

Once logged in, a menu will appear, and you can choose witch operation to do. Here's an 101 of how they work:
- add <wine> <image> : Adds a wine to the app with the name wine and the image.
- sell <wine> <value> <quantity> : Puts a wine with the name wine for sale, selling each bottle for value. quantity bottles
will be put up for sale.
- view <wine> : Shows the info about wine, such as name, file image name, avg of ratings(1-5) and, if exists, any sale of that
wine (seller, price for bottle and quantity).
- buy <wine> <seller> <quantity> : Buys quantity bottles of wine with name wine to the seller.
- wallet : Shows the balance
- classify <wine> <stars> : Classifies the wine with star.
- talk <user> : Sends a message to user.
- read : Shows all the messages received
- list : Lists all the transactions executed on the app
- exit : Exits the app

## Limitations

Here are some of the operation limitations:
- add <wine> <image> : wine must be a String and image must be a String path to the file containing the image. It's imperative
that the image exists in the client_files directory, otherwise client is shutdown.
- sell <wine> <value> <quantity> : wine must be a String, while value and quantity both must be positive Integers. It's only
possible to sell existing wines.
- view <wine> : wine must be a String and wine must exist.
- buy <wine> <seller> <quantity> : wine must be a String and wine must exist. seller must be a String and seller must be an
existing user. Also, seller cant be the user itself. quantity must be a positive Integer.
- classify <wine> <stars> : wine must be a String and wine must exist.
- talk <user> : user must be a String and user must exist. user cannot be the user itself

### Other limitations:
- On a first running of the server, everything works smoothly. However, when the server is restarted, only one user can connect
to the server before the server crashes due to a BadPaddingException.
- Read functionality does not work. It raises an InvalidKeyException: Unwrapping failed uppon executing the read function.
- The blockchain is not verified correctly by the server after restart. The signature os each block is invalid and therefore,
we decided to comment the code snippet that evaluates the server signature on each block.


## Other explanations
File organization:

### data_bases
 
Contains the .txt files that store all data from the app.
- balance.txt contains the balance of every registered user in clientID;balance format.
- forSale.txt contains all sales from all users in wineName;seller;price;quantity format.
- wines.txt contains all added wines in format wineName;imageFileName;[stars] format.

These files are updated through the app execution, by structures in the server memory.

### client_files

Contains image files of the wines to be added. When view operation is called, the file that contains the image of the wine
passed as arg in the view operation is overwritten.

### server_files

Contains image files of the wines added.

### blockchain

Contains all the block files that contain each one each transaction executed on the app, to a max of 5 transactions per block file.

### certificates

Contains all the certificates exctracted from the users keystores.

### chat

Contains all the chat files encrypted in the <userX_chat> format, where X is the receiver's id. Only the user with the receiver's id
is able to decrypt the file with the messages sent to him.

### chat_keys

Contains the keys to decrypt the encrypted chat files, in the <userX_chat_keys> format.

### keystores

Contains all the keystores, including the servers, the users ones and the app truststore.

### Server memory (Local memory)

There are structures used to operate in the databases. They are updated from the data_bases directory .txt files when the server starts.
- ArrayList<Utilizador> listaUts : Stores all registered users.
- ArrayList<Wine> listaWines : Stores all added wines.
- HashMap<Utilizador,ArrayList<Sale>> forSale : Stores all sales, mapping each list of sales to its seller (user).

## Other classes

- Utilizador is an object that represents a user, defined by an id clientID and a balance.
- Wine is a record that represents a wine, defined by a name, image file name image and all ratings (1-5) stars.
- Sale is an object that represents a sale, defined by a wine, a unitary price value and a quantity.
- Transacao is an object that represents a transaction, defined by wine name, a quantity, a unitary price value, a user id and a transaction type.
- Blockchain is an object that represents a blockchain, defined by a list of blocks.
- Block is an object that represents a block, defined by a hash, an id, number of transactions, a list with the transactions,
a signature and the block data signed by the server's signature.
- TransacaoType is an enumeration object that represents the two types of transaction, buy or sell.


## How to Run

### Setup

Firstly, if you haven't already created the keystores, run the setup bash file to create pre-configured keystores,
truststore and certificates.

    $ ./setup.sh


If you already have the keystores and certificates:

To compile and run server:

    $ java -jar TintolmarketServer.jar [port] <password-cifra> <keystore> <password-keystore>


To compile and run client:

    $ java -jar Tintolmarket.jar <serverAddress>:[port] <truststore> <keystore> <password-keystore> <userID>


## 

Segurança e Confiabilidade @ Faculdade de Ciencias, University of Lisbon
