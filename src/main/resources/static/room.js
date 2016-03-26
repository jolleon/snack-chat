var Room = Backbone.Model.extend({});

var Rooms = Backbone.Collection.extend({
    model: Room,
    url: '/api/rooms'
});

var RoomView = Backbone.View.extend({
    tagName: 'li',
    initialize: function() {
        _.bindAll(this, 'render');
    },
    render: function() {
        $(this.el).html('<a href="#room/' + this.model.get('id') + '">' + this.model.get('name') + "</a>");
        return this;
    }
});

var rooms = new Rooms;

var RoomsView = Backbone.View.extend({
    el: $('body'),

    initialize: function(){
        _.bindAll(this, 'render');
        this.collection = rooms;
    },

    render: function(){
        $('ul', this.el).html("");
        var self = this;
        _(this.collection.models).each(function(room){
            self.appendItem(room);
        }, this);
    },

    appendItem: function(room){
        var roomView = new RoomView({
            model: room
        })
        $('ul', this.el).append(roomView.render().el);
    }
});

var roomsView = new RoomsView();

var refreshRooms = function(){
    rooms.fetch({success: roomsView.render});
};



var Message = Backbone.Model.extend({});

var Messages = Backbone.Collection.extend({
    model: Message,

    url: function() {
        return '/api/rooms/' + this.roomId + '/messages';
    },

    initialize: function(){
        this.roomId = 0;
    }
});

var messages = new Messages();


var MessageView = Backbone.View.extend({
    tagName: 'div',
    initialize: function(){
        _.bindAll(this, 'render');
    },
    render: function(){
        $(this.el).html(`
        <div class="message">
            <div class="author col-sm-2 col-md-2">${this.model.get('author')}</div>
            <div class="col-sm-10 col-md-10">${this.model.get('content')}</div>
        </div>
        `);
        return this;
    }
});

var MessagesView = Backbone.View.extend({
    el: $('#messages'),

    initialize: function(){
        _.bindAll(this, 'render');
        this.collection = messages;
    },

    render: function(){
        $(this.el).html("");
        var self = this;
        _(this.collection.models).each(function(message){
            self.appendItem(message);
        }, this);

        var r = rooms.get(messages.roomId);
        $(".room-header").html("<h2>" + r.get("name") + "</h2>");
    },

    appendItem: function(message){
        var messageView = new MessageView({
            model: message
        })
        $(this.el).append(messageView.render().el);
    }
});

messagesView = new MessagesView();
var refreshMessages = function() {
    messages.fetch({success: messagesView.render})
};


var warningFlash = function(el, t, n) {
    if(n==0){
        return;
    }
    var old = el.css("border");
    el.css("border", "solid 3px red");
    setTimeout(function(){
        el.css("border", old);
        setTimeout(function(){warningFlash(el, t, n-1)}, t);
    }, t);
};

var FormView = Backbone.View.extend({
    el: $("#inputText"),

    events: {
        "submit form": "submit",
        "keyup": "processKey"
    },

    processKey: function(e) {
      if(e.which === 13) // enter key
        this.submit();
    },

    submit: function(){
        var author = $("#username").val();
        var content = $("#newMessage").val();
        if (author.length < 1) {
            warningFlash($("#username"), 100, 5);
        };
        if (content.length < 1) {
            warningFlash($("#newMessage"), 100, 5);
        };
        if(author.length < 1 || content.length < 1) {
            return;
        };
        var m = new Message();
        m.set({"author": author, "content": content});
        messages.add(m);
        m.save();
        $("#newMessage").val("");
        messagesView.render();
    }
});

new FormView();


var Workspace = Backbone.Router.extend({
    routes: {
        "room/:roomId": "room"
    },

    room: function(roomId) {
        console.log("entering room " + roomId);
        messages.roomId = roomId;
        refreshMessages();
    }
});

var router = new Workspace;

Backbone.history.start();

refreshRooms();
refreshMessages();

setInterval(refreshMessages, 1000);
