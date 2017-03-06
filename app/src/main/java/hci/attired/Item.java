package hci.attired;

/**
 * Created by hamzamalik0123 on 06/03/2017.
 */

class Item {
    private int id;
    private String name, price, image, desc;

    public Item(int id, String name, String price, String image, String desc) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}