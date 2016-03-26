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
        }, this)
    },

    appendItem: function(room){
        console.log(room);
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

refreshRooms();



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
        console.log("rendering");
        console.log(this.collection.models)
        $(this.el).html("");
        var self = this;
        _(this.collection.models).each(function(message){
            self.appendItem(message);
        }, this)
    },

    appendItem: function(message){
        console.log("appending item");
        console.log(message);
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

refreshMessages();



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
