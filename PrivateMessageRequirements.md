#this is my first go at the requirements for the private message feature

# Introduction #

May not be complete


# Details #

A.	**Problem.** Sometimes one user will want to communicate directly with another user. SimpleChat currently has no facility to handle private messages between individual users.

B.	**Background Information.** This requirement will be based on top of the requirements for SimpleChat Phase2. In Phase2, any user with the client program could send a message to the server and the server then echoed that message to all of the connected clients. The feature of this requirement will be for a user on a client program to send a message to the server; inside this message will be the contents of the message along with a specified user to send it to.

Some issues:

**[Issue 1](https://code.google.com/p/plusimplechat/issues/detail?id=1):**	How will the server know that it is receiving a private message?

_Decision:_	Add a command, #private that the server will recognize as not being a message to send to all clients.


**[Issue2](https://code.google.com/p/plusimplechat/issues/detail?id=2):**	How will the server know where to send the message?

_Decision:_	The first token after the “#private” command sent by the client will contain the desired recipient’s loginID. The rest of the message after the loginID specification will be sent to that user.

**[Issue3](https://code.google.com/p/plusimplechat/issues/detail?id=3):**	What if the specified user does not exist or is not connected.

_Decision:_	Server sends a message back to the sender indicating that the message was not sent and that the recipient is either unavailable or does not exist.

**[Issue4](https://code.google.com/p/plusimplechat/issues/detail?id=4):**	How does the receiving user know they are receiving a private message?

_Decision:_	When the server sends a private message to the recipient , the message is prefixed with the sender’s loginid followed by a colon. Now the user will know that they are receiving a private message and what loginid to specify if they should decide to respond.

**[Issue5](https://code.google.com/p/plusimplechat/issues/detail?id=5):**	Can a user send private messages to themselves?

_Decision:_	Yes, if they’re bored.

**[Issue6](https://code.google.com/p/plusimplechat/issues/detail?id=6):**	Can a user of the server program send private messages?

_Decision:_	Yes, This will be useful for chat room moderation.

**[Issue7](https://code.google.com/p/plusimplechat/issues/detail?id=7):**	Can a user send private messages to the server?

_Decision:_	???

C.	**Environment and system models.** This feature is in addition to Phase2 and will be added at in conjunction with blocking, forwarding and channels. No additional environment considerations for this requirement.

D.	**Functional Requirements.**

-This command can be entered into the user interface of either the client or the server program. The message in this format will be sent to the server program as it is.

#private <user loginid> 

&lt;message&gt;



> -send 

&lt;message&gt;

 to <user loginid>

> -does not send if <user loginid> does not exist

> -does not send if <user loginid> is not connected

-if not sent, a message is sent to the user who entered the command that indicates why the message was not sent.

-if message sent, <user loginid> receives a message prefixed by senders login id , 

&lt;sender&gt;

.
> 

&lt;sender&gt;

: 

&lt;message&gt;



-if the message is from the server, the message will be prefixed by “Server:”

-if sender sends 

&lt;message&gt;

 to 

&lt;sender&gt;

, the message will be prefixed by “Me: “

E.	**Other Requirements.** There are no additional process, quality, or platform requirements specific to this feature.