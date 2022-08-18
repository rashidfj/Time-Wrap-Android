package com.peek.time.wrap.scan.timewrap.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.activities.ImageOpenActivity;
import com.peek.time.wrap.scan.timewrap.activities.SavedImagesActivity;
import com.peek.time.wrap.scan.timewrap.model.LiveDataModel;
import com.peek.time.wrap.scan.timewrap.model.SavedModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.viewHolder> {


    private final List<SavedModel> imagesList;
    private Context context;


    LiveDataModel savedMainViewModel;
    boolean isEnable = false;
    boolean isSelectAll = false;
    List<SavedModel> selectedItems = new ArrayList<>();
    SparseBooleanArray savedSelectedItemsIds = new SparseBooleanArray();
    MenuItem menu_select_all;


    public SavedAdapter(List<SavedModel> imagesList, Context context) {
        this.imagesList = imagesList;
        this.context = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.cell_saved_sdapter, parent, false);

        savedMainViewModel = ViewModelProviders.of((FragmentActivity) context).get(LiveDataModel.class);

        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, @SuppressLint("RecyclerView") int position) {

        final SavedModel status = imagesList.get(holder.getAdapterPosition());
        context = holder.itemView.getContext();

        Picasso.get().load(status.getFile()).into(holder.img_thumb);

        holder.img_thumb.setOnClickListener(v -> {

            if (isEnable) {
                SavedClickItem(holder, null);
            } else {
                Intent intent = new Intent(context, ImageOpenActivity.class);
                intent.putExtra("STR_IMAGE", imagesList.get(position).getPath());
                context.startActivity(intent);
            }
        });


        holder.img_share.setOnClickListener(view -> {

            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + status.getFile().getAbsolutePath()));
            context.startActivity(Intent.createChooser(shareIntent, "Share with"));

        });


        holder.btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (status.getFile().delete()) {
                    imagesList.remove(position);
                    notifyDataSetChanged();
                    Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(context, "Unable to Delete File", Toast.LENGTH_SHORT).show();

            }
        });


        if (savedSelectedItemsIds.get(holder.getAdapterPosition(), false)) {

            holder.layout_all_check.setVisibility(View.VISIBLE);

        } else {
            holder.layout_all_check.setVisibility(View.GONE);

        }

        holder.img_thumb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                Log.d("Imageclicked123", "clicked");


                if (!isEnable) {
                    ActionMode.Callback callback = new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

                            MenuInflater menuInflater = actionMode.getMenuInflater();
                            menuInflater.inflate(R.menu.menu_main, menu);
                            SavedImagesActivity.statusActionMode = actionMode;
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                            menu_select_all = menu.findItem(R.id.menu_select_all);

                            isEnable = true;
                            SavedClickItem(holder, actionMode);

                            savedMainViewModel.getText().observe((LifecycleOwner) context
                                    , new Observer<String>() {
                                        @Override
                                        public void onChanged(String s) {
                                            actionMode.setTitle(String.format("%s Selected", s+"/"+imagesList.size()));

                                            if (selectedItems.isEmpty()) {
                                                menu_select_all.setVisible(true);
                                            } else {
                                                menu_select_all.setVisible(selectedItems.size() != imagesList.size());
                                            }

                                        }
                                    });

                            return true;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                            int id = menuItem.getItemId();
                            switch (id) {
                                case R.id.action_delete:

                                    androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(
                                            new ContextThemeWrapper(context, R.style.CustomAlertDialog));

                                    alert.setTitle("Delete");
                                    alert.setMessage("Are you sure you want to delete the selected item ?");
                                    alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                            for (SavedModel s : selectedItems) {
                                                imagesList.remove(s);
                                                s.getFile().delete();
                                                notifyDataSetChanged();
                                            }
                                            if (actionMode != null) {
                                                actionMode.finish();
                                            }

                                        }
                                    });
                                    alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // close dialog
                                            dialog.cancel();
                                            if (actionMode != null) {
                                                actionMode.finish();
                                            }
                                        }
                                    });
                                    alert.show();
                                    break;


                                case R.id.menu_select_all:
                                    if (selectedItems.size() == imagesList.size()) {
                                        isSelectAll = false;
                                        selectedItems.clear();
                                        savedSelectedItemsIds.clear();
                                    } else {
                                        isSelectAll = true;
                                        selectedItems.clear();
                                        selectedItems.addAll(imagesList);
                                        for (int i = 0; i < imagesList.size(); i++) {
                                            savedSelectedItemsIds.put(i, true);
                                        }
                                        menu_select_all.setVisible(false);
                                        /*      menu_select_all_2.setVisible(true);*/
                                    }
                                    savedMainViewModel.setText(String.valueOf(selectedItems.size()));
                                    notifyDataSetChanged();
                                    break;

                            }

                            return true;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode actionMode) {

                            isEnable = false;
                            isSelectAll = false;
                            selectedItems.clear();
                            notifyDataSetChanged();
                            savedSelectedItemsIds.clear();
                        }
                    };

                    ((AppCompatActivity) view.getContext()).startActionMode(callback);
                }
                return true;

            }
        });


    }

    @Override
    public int getItemCount() {
        return imagesList.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        RelativeLayout relative_whatsapp, layout_all_check;
        ImageView img_thumb, btn_download, img_share;

        public viewHolder(@NonNull View itemView) {
            super(itemView);


            relative_whatsapp = itemView.findViewById(R.id.relative_whatsapp);
            img_thumb = itemView.findViewById(R.id.thumb_whatsapp);
            btn_download = itemView.findViewById(R.id.download_whatsapp_btn);
            img_share = itemView.findViewById(R.id.img_share_whatsapp);
            layout_all_check = itemView.findViewById(R.id.select_all_layout);
        }
    }


    private void SavedClickItem(viewHolder holder, ActionMode mode) {

        SavedModel s = imagesList.get(holder.getAdapterPosition());
        if (holder.layout_all_check.getVisibility() == View.GONE) {

            holder.layout_all_check.setVisibility(View.VISIBLE);
            selectedItems.add(s);
            savedSelectedItemsIds.put(holder.getAdapterPosition(), true);
        } else {
            holder.layout_all_check.setVisibility(View.GONE);
            selectedItems.remove(s);
            savedSelectedItemsIds.delete(holder.getAdapterPosition());

            if (mode != null) {
                mode.finish();

            }
        }
        savedMainViewModel.setText(String.valueOf(selectedItems.size()));
    }
}
