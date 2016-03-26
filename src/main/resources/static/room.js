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
        $(this.el).html('<a href="/room/' + this.model.get('id') + '">' + this.model.get('name') + "</a>");
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
