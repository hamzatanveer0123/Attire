package hci.attired;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import static hci.attired.R.id.itemImage;

/**
 * Created by hamzamalik0123 on 06/03/2017.
 */

class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder>{

    private Context context;
    private List<Item> list;

    public CustomAdapter(Context context, List<Item> list) {
        this.context = context;
        this.list    = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int index) {
        holder.textview.setText(list.get(index).getDesc());
        Picasso.with(context).load(list.get(index).getImage()).resize(500, 500).centerCrop().into(holder.imageview);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageview;
        public TextView textview;

        public ViewHolder(View itemView) {
            super(itemView);
            textview = (TextView) itemView.findViewById(R.id.itemDetails);
            imageview = (ImageView) itemView.findViewById(itemImage);

        }
    }
}
